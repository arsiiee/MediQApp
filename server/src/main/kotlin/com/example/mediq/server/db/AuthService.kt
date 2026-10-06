package com.example.mediq.server.db

import com.example.mediq.domain.model.UserProfile
import com.example.mediq.domain.model.UserRole
import com.example.mediq.server.ApiError
import com.example.mediq.server.Database
import com.example.mediq.server.auth.Passwords
import com.example.mediq.server.auth.Tokens
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Sign-in and the three-step registration flow.
 *
 * The OTP exists to prove the patient controls the phone number. Two properties
 * matter and both are enforced in SQL rather than in application code:
 *
 *  - the code is stored hashed, so a database copy does not hand over live
 *    one-time codes;
 *  - `consumed_at` is set in the same update that verifies the code, so a
 *    captured code cannot be replayed.
 *
 * There is no SMS provider wired up. [request] generates a code and returns it
 * in the response so the flow can be tested, and that is the one part of this
 * file that must change before real use — see [deliverCodeForDevelopment].
 */
class AuthService(
    private val db: Database,
    private val users: UserStore,
    private val tokens: Tokens,
    private val tokenTtlMinutes: Long,
    private val otpTtlMinutes: Long,
) {

    private val random = SecureRandom()

    fun signIn(username: String, password: String): Pair<AccessToken, UserRow> {
        val user = users.findByUsername(normalizeUsername(username))

        // Always run the hash comparison, even when there is no such user, so a
        // wrong username and a wrong password take the same time. Bailing out
        // early on an unknown username — which is the obvious way to write this
        // — would make the response time reveal exactly which usernames exist.
        val stored = user?.passwordHash ?: DUMMY_HASH
        val passwordMatches = Passwords.verify(password, stored)

        if (user == null || !passwordMatches) {
            // One message for both failures, so the response body gives nothing
            // away either.
            throw ApiError.unauthorized("Those details don't match an account.")
        }

        val expiresAt = Instant.now().plusSeconds(tokenTtlMinutes * 60)
        val sessionId = users.createSession(user.id, expiresAt)
        val token = tokens.issue(sessionId, user.id, user.role.wireValue, expiresAt)
        return AccessToken(token, expiresAt) to user
    }

    fun signOut(sessionId: String) = users.revokeSession(sessionId)

    /** Resolves a bearer token to a live user, or null if the session is dead. */
    fun authenticate(token: String): UserRow? {
        val claims = tokens.verify(token) ?: return null
        // The token being valid is not enough — the session must still be live,
        // or sign-out would leave a usable token behind.
        val userId = users.activeSession(claims.sessionId) ?: return null
        return users.findById(userId)
    }

    /**
     * Starts registration by issuing a code for [mobileNumber].
     *
     * Returns the code so the flow can be exercised without an SMS gateway.
     * Delete [returnCodeToCaller] before this reaches real patients.
     */
    fun requestOtp(mobileNumber: String): Pair<String, Boolean> {
        val normalized = normalizeMobile(mobileNumber)

        // Re-issue rather than reuse, and invalidate anything outstanding so
        // only the newest code works.
        db.tx { c ->
            c.prepareStatement(
                "UPDATE otp_codes SET consumed_at = ? WHERE mobile_number = ? AND consumed_at IS NULL"
            ).use { st ->
                st.setObject(1, OffsetDateTime.now(ZoneOffset.UTC))
                st.setString(2, normalized)
                st.executeUpdate()
            }
        }

        val code = (100_000 + random.nextInt(900_000)).toString()
        val expiresAt = Instant.now().plusSeconds(otpTtlMinutes * 60)

        db.tx { c ->
            c.prepareStatement(
                """
                INSERT INTO otp_codes (id, mobile_number, code_hash, expires_at, created_at)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { st ->
                st.setString(1, UUID.randomUUID().toString())
                st.setString(2, normalized)
                st.setString(3, Passwords.hash(code))
                st.setObject(4, expiresAt.atOffset(ZoneOffset.UTC))
                st.setObject(5, OffsetDateTime.now(ZoneOffset.UTC))
                st.executeUpdate()
            }
        }

        deliverCodeForDevelopment(normalized, code)
        return code to returnCodeToCaller
    }

    /**
     * Checks the code and returns a registration id.
     *
     * The returned id is the proof the number was verified. [register] will not
     * accept an account without one, so skipping this step is not possible by
     * calling a different endpoint.
     */
    /**
     * Outcome of the read-and-decide phase, returned from the transaction so
     * the failure paths can act on it *after* the transaction has closed.
     *
     * [wrongCodeRowId] is the row whose attempt counter must be bumped. It is
     * carried out of the transaction rather than incremented inside it: a throw
     * inside `db.tx` rolls back, which would erase the increment and leave the
     * lockout ineffective. The increment then runs in its own short
     * transaction, so a wrong code costs two sequential checkouts instead of
     * two *simultaneous* ones.
     */
    private sealed interface OtpAttempt {
        /** The code was wrong; [rowId] must have its attempt counter bumped. */
        data class WrongCode(val rowId: String) : OtpAttempt

        /** The code was right; [rowId] has already been consumed. */
        data class Verified(val rowId: String) : OtpAttempt
    }

    fun verifyOtp(mobileNumber: String, otp: String): String {
        val normalized = normalizeMobile(mobileNumber)

        val outcome = db.tx { c ->
            val row = c.prepareStatement(
                """
                SELECT id, code_hash, attempt_count, expires_at
                FROM otp_codes
                WHERE mobile_number = ? AND consumed_at IS NULL
                ORDER BY created_at DESC
                LIMIT 1
                """.trimIndent()
            ).use { st ->
                st.setString(1, normalized)
                st.executeQuery().use { rs ->
                    if (rs.next()) {
                        Row(
                            id = rs.getString("id"),
                            hash = rs.getString("code_hash"),
                            attempts = rs.getInt("attempt_count"),
                            expiresAt = rs.getObject("expires_at", OffsetDateTime::class.java).toInstant(),
                        )
                    } else {
                        null
                    }
                }
            } ?: throw ApiError.badRequest("Request a code first.")

            if (row.expiresAt.isBefore(Instant.now())) {
                throw ApiError.badRequest("That code has expired. Request a new one.")
            }
            // Lock the attempt count out after five guesses, so a six-digit
            // code cannot be brute-forced through the API.
            if (row.attempts >= MAX_OTP_ATTEMPTS) {
                throw ApiError.tooManyAttempts("Too many wrong codes. Request a new one.")
            }

            if (!Passwords.verify(otp, row.hash)) {
                // Returned, not thrown: throwing here would roll back and lose
                // the increment, and opening a nested db.tx while this one
                // holds a connection would need a second one from the pool at
                // the same time. POST /auth/otp/verify is unauthenticated, so
                // five parallel wrong codes must not be able to exhaust a
                // 10-connection pool and stall every other route.
                return@tx OtpAttempt.WrongCode(row.id)
            }

            // Consume in the same transaction that verified it.
            c.prepareStatement("UPDATE otp_codes SET consumed_at = ? WHERE id = ?").use { st ->
                st.setObject(1, OffsetDateTime.now(ZoneOffset.UTC))
                st.setString(2, row.id)
                st.executeUpdate()
            }

            // The registration id *is* the consumed code row. It cannot be
            // reused, and it carries the verified number with it.
            OtpAttempt.Verified(row.id)
        }

        return when (outcome) {
            is OtpAttempt.Verified -> "reg_${outcome.rowId}"
            is OtpAttempt.WrongCode -> {
                // Own transaction, sequential with the read above, so the
                // counter survives the throw below.
                db.tx { c ->
                    c.prepareStatement("UPDATE otp_codes SET attempt_count = attempt_count + 1 WHERE id = ?")
                        .use { st ->
                            st.setString(1, outcome.rowId)
                            st.executeUpdate()
                        }
                }
                throw ApiError.badRequest("That code isn't right.")
            }
        }
    }

    fun register(
        registrationId: String,
        fullName: String,
        username: String,
        password: String,
        dateOfBirth: LocalDate?,
        sex: com.example.mediq.domain.model.Sex?,
        email: String?,
    ): Pair<AccessToken, UserRow> {
        // Normalized once here so the uniqueness check and the stored row are
        // guaranteed to agree with what [signIn] later looks up.
        val normalizedUsername = normalizeUsername(username)
        val codeRowId = registrationId.removePrefix("reg_")
        val mobileNumber = db.tx { c ->
            val row = c.prepareStatement(
                """
                SELECT mobile_number FROM otp_codes
                WHERE id = ? AND consumed_at IS NOT NULL AND consumed_at > ?
                """.trimIndent()
            ).use { st ->
                st.setString(1, codeRowId)
                st.setObject(2, OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(REGISTRATION_WINDOW_SECONDS))
                st.executeQuery().use { rs -> if (rs.next()) rs.getString("mobile_number") else null }
            } ?: throw ApiError.badRequest("Verify your number first, or start again.")

            if (users.usernameExists(normalizedUsername)) {
                throw ApiError.badRequest("That username is already taken.")
            }
            if (email != null && users.emailExists(email)) {
                throw ApiError.badRequest("That email is already registered.")
            }
            if (users.mobileExists(row)) {
                throw ApiError.badRequest("An account already exists for that number.")
            }
            if (dateOfBirth == null) {
                throw ApiError.badRequest("A date of birth is required.")
            }
            if (password.length < MIN_PASSWORD_LENGTH) {
                throw ApiError.badRequest("Use a password of at least $MIN_PASSWORD_LENGTH characters.")
            }
            row
        }

        val user = users.create(
            username = normalizedUsername,
            password = password,
            fullName = fullName,
            email = email,
            mobileNumber = mobileNumber,
            // `users.create` requires a non-null date of birth. The check above
            // runs inside a transaction lambda, where a smart cast of the
            // captured parameter does not reach this call site, so the null case
            // is turned into the same error here rather than a null sneaking in.
            dateOfBirth = dateOfBirth ?: throw ApiError.badRequest("A date of birth is required."),
            sex = sex,
        )

        val expiresAt = Instant.now().plusSeconds(tokenTtlMinutes * 60)
        val sessionId = users.createSession(user.id, expiresAt)
        val token = tokens.issue(sessionId, user.id, user.role.wireValue, expiresAt)
        return AccessToken(token, expiresAt) to user
    }

    private fun normalizeMobile(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.matches(Regex("^\\+?[0-9]{10,15}$"))) {
            throw ApiError.badRequest("Enter a valid mobile number in international format.")
        }
        return if (trimmed.startsWith("+")) trimmed else "+$trimmed"
    }

    /**
     * Normalizes a username for storage and lookup alike.
     *
     * Applied on both sides so the two can never disagree: [register] stores
     * this form and [signIn] looks up this form.
     *
     * Trimming matters more than case. A phone keyboard that autocorrects or
     * autocompletes a username can leave a trailing space or an uppercase first
     * letter, and matching raw meant correct credentials produced the same
     * opaque "Those details don't match an account." as a genuinely wrong
     * password — with no way for a patient to tell the two apart.
     *
     * An empty result is rejected rather than looked up: an empty username must
     * not fall through to matching nothing while appearing to be tried.
     */
    private fun normalizeUsername(raw: String): String {
        val trimmed = raw.trim().lowercase()
        if (trimmed.isEmpty()) {
            throw ApiError.badRequest("Enter your username.")
        }
        return trimmed
    }

    /** Stub for the SMS gateway. Must send the code before real use. */
    private fun deliverCodeForDevelopment(mobileNumber: String, code: String) {
        if (returnCodeToCaller) {
            println("[MediQ] OTP for $mobileNumber is $code")
        }
    }

    private data class Row(val id: String, val hash: String, val attempts: Int, val expiresAt: Instant)

    private companion object {
        const val MAX_OTP_ATTEMPTS = 5
        const val MIN_PASSWORD_LENGTH = 8

        /** How long a verified registration id stays usable. */
        const val REGISTRATION_WINDOW_SECONDS = 15 * 60L

        /**
         * True means the OTP is printed and returned in the HTTP response.
         * There is no SMS provider; set to false once one is wired up.
         */
        val returnCodeToCaller = true

        /** A real hash of a random string, so the dummy path still costs the same. */
        val DUMMY_HASH: String by lazy { Passwords.hash(UUID.randomUUID().toString()) }
    }
}

data class AccessToken(val value: String, val expiresAt: Instant)