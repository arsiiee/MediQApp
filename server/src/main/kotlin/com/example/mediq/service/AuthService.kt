package com.example.mediq.service

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.example.mediq.security.JwtProvider
import com.example.mediq.security.PasswordHash
import com.example.mediq.util.isBefore
import com.example.mediq.util.plusDays
import com.example.mediq.util.plusMinutes
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory

class AuthService(
    private val authRepository: AuthRepository,
    private val jwtProvider: JwtProvider,
    private val passwordHash: PasswordHash,
    private val emailProvider: EmailProvider,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    private fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    suspend fun signIn(username: String, password: String): LoadState<AuthSession> {
        return authRepository.signIn(username, password).fold(
            onSuccess = { session ->
                val user = session.profile
                if (user.status != AccountStatus.ACTIVE) {
                    LoadState.Error(ApiFailure.forbidden("Account is not active"))
                } else {
                    createSession(user)
                }
            },
            onError = { LoadState.Error(it) },
        )
    }

    suspend fun register(request: RegisterRequest): LoadState<AuthSession> {
        return authRepository.findByUsername(request.username).fold(
            onSuccess = { existing ->
                if (existing != null) {
                    LoadState.Error(ApiFailure.conflict("Username already exists"))
                } else if (request.email.isNotBlank()) {
                    authRepository.findByContact(request.email).fold(
                        onSuccess = { contactExisting ->
                            if (contactExisting != null) {
                                LoadState.Error(ApiFailure.conflict("email already in use"))
                            } else {
                                authRepository.register(request).fold(
                                    onSuccess = { session -> createSession(session.profile) },
                                    onError = { LoadState.Error(it) },
                                )
                            }
                        },
                        onError = { LoadState.Error(it) },
                    )
                } else {
                    authRepository.register(request).fold(
                        onSuccess = { session -> createSession(session.profile) },
                        onError = { LoadState.Error(it) },
                    )
                }
            },
            onError = { LoadState.Error(it) },
        )
    }

    /**
     * Issues a one-time code for a contact.
     *
     * There is no SMS gateway behind this server yet, so the code is written
     * to the log. A real deployment sends it instead and changes nothing else.
     */
    suspend fun requestOtp(request: RequestOtpRequest): LoadState<Unit> {
        val contact = request.contact.trim()
        if (contact.isEmpty()) {
            return LoadState.Error(ApiFailure.validation("Enter the number you signed up with."))
        }

        val code = generateOtpCode()
        authRepository.saveOtp(otpKey(contact, request.purpose), code, now().plusMinutes(OTP_VALIDITY_MINUTES))

        if (contact.contains("@")) {
            emailProvider.send(
                to = contact,
                subject = "Your MediQ verification code",
                body = "Your MediQ verification code is $code. It expires in $OTP_VALIDITY_MINUTES minutes."
            )
        } else {
            logger.info("OTP for $contact (${request.purpose}): $code")
        }

        return LoadState.Success(Unit)
    }

    /**
     * Checks a one-time code.
     *
     * A code works once and only for the contact and purpose it was issued
     * for. For a password reset the account is returned with a session, so the
     * patient can go on to set a new password.
     */
    suspend fun verifyOtp(request: VerifyOtpRequest): LoadState<AuthSession> {
        val contact = request.contact.trim()
        val key = otpKey(contact, request.purpose)
        val entry = authRepository.findOtp(key)
            ?: return LoadState.Error(ApiFailure.validation("That code has expired. Request a new one."))

        if (entry.expiresAt.isBefore(now())) {
            authRepository.deleteOtp(key)
            return LoadState.Error(ApiFailure.validation("That code has expired. Request a new one."))
        }

        if (entry.code != request.code.trim()) {
            return LoadState.Error(ApiFailure.validation("That code is not right. Check the six digits and try again."))
        }

        // A code is good for one attempt.
        authRepository.deleteOtp(key)

        return when (request.purpose) {
            OtpPurpose.RESET_PASSWORD -> {
                val user = authRepository.findByContact(contact).getOrNull()
                    ?: return LoadState.Error(ApiFailure.notFound("We have no account for that number."))
                createSession(user)
            }

            OtpPurpose.REGISTER -> {
                /*
                 * The account is created by the credentials step, which comes
                 * after this one. This only proves the number belongs to the
                 * patient, so the session is a placeholder.
                 */
                LoadState.Success(pendingSession(contact))
            }
        }
    }

    /** Sets a new password for the signed-in account. */
    suspend fun updatePassword(userId: String, newPassword: String): LoadState<Unit> {
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            return LoadState.Error(ApiFailure.validation("Use at least $MIN_PASSWORD_LENGTH characters."))
        }

        val hash = passwordHash.hash(newPassword)
        return authRepository.updatePassword(userId, hash).fold(
            onSuccess = { LoadState.Success(Unit) },
            onError = { LoadState.Error(it) },
        )
    }

    private fun otpKey(contact: String, purpose: OtpPurpose) = "${purpose.name}:${contact.lowercase()}"

    private fun generateOtpCode(): String =
        java.util.Random().nextInt(100_000, 1_000_000).toString()

    /** A session that only proves a number was verified. */
    private fun pendingSession(contact: String): AuthSession {
        val issuedAt = now()
        return AuthSession(
            accessToken = "",
            expiresAt = issuedAt,
            refreshToken = "",
            refreshExpiresAt = issuedAt,
            profile = UserProfile(
                id = "pending-$contact",
                username = contact,
                fullName = contact,
                email = null,
                mobileNumber = contact,
                dateOfBirth = LocalDate.parse("1970-01-01"),
                role = UserRole.PATIENT,
                status = AccountStatus.PENDING_VERIFICATION,
                createdAt = issuedAt,
                updatedAt = issuedAt,
            ),
        )
    }

    suspend fun refreshToken(refreshToken: String): LoadState<AuthSession> {
        return authRepository.refreshToken(refreshToken).fold(
            onSuccess = { session ->
                val newAccessToken = jwtProvider.createAccessToken(session.profile)
                val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                val newRefreshToken = jwtProvider.createRefreshToken(session.profile.id)
                val newSession = session.copy(
                    accessToken = newAccessToken,
                    expiresAt = now.plusMinutes(15),
                    refreshToken = newRefreshToken,
                    refreshExpiresAt = now.plusDays(30),
                )
                authRepository.storeRefreshToken(newRefreshToken, session.profile.id, now.plusDays(30))
                authRepository.revokeToken(refreshToken)
                LoadState.Success(newSession)
            },
            onError = { LoadState.Error(it) },
        )
    }

    suspend fun findById(id: String): LoadState<UserProfile> = authRepository.findById(id).fold(
        onSuccess = { profile ->
            if (profile != null) {
                LoadState.Success(profile)
            } else {
                LoadState.Error(ApiFailure.notFound("User not found"))
            }
        },
        onError = { LoadState.Error(it) },
    )

    suspend fun updateProfile(profile: UserProfile): LoadState<UserProfile> = authRepository.updateProfile(profile)

    private suspend fun createSession(profile: UserProfile): LoadState<AuthSession> {
        val accessToken = jwtProvider.createAccessToken(profile)
        val refreshToken = jwtProvider.createRefreshToken(profile.id)
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        authRepository.storeRefreshToken(refreshToken, profile.id, now.plusDays(30))
        return LoadState.Success(AuthSession(
            accessToken = accessToken,
            expiresAt = now.plusMinutes(15),
            refreshToken = refreshToken,
            refreshExpiresAt = now.plusDays(30),
            profile = profile,
        ))
    }

    private companion object {
        /** How long a one-time code stays usable. */
        const val OTP_VALIDITY_MINUTES = 5

        /** The shortest password we accept. */
        const val MIN_PASSWORD_LENGTH = 8
    }
}
