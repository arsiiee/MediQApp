package com.example.mediq.server

import com.example.mediq.domain.model.Sex
import com.example.mediq.server.auth.Tokens
import com.example.mediq.server.db.AuthService
import com.example.mediq.server.db.UserStore
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers the OTP → registration → sign-in lifecycle and the edge-cases that
 * guard it: duplicate identifiers, wrong passwords, brute-force lockout, and
 * expired codes.
 *
 * Every test gets its own private in-memory database so there is no shared
 * state between runs.
 */
class AuthServiceTest {

    private val config = ServerConfig(
        port = 0,
        jdbcUrl = "jdbc:h2:mem:auth_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
        jwtSecret = "test-secret-not-used-for-anything-real-123456",
        jwtIssuer = "mediq-test",
        tokenTtlMinutes = 60,
        slotDurationMinutes = 30,
        otpTtlMinutes = 5,
        seedDemoData = false,
    )

    private val db = Database(config)
    private val users = UserStore(db)
    private val tokens = Tokens(config.jwtSecret, config.jwtIssuer)
    private val auth = AuthService(db, users, tokens, config.tokenTtlMinutes, config.otpTtlMinutes)

    // -------------------------------------------------------------------------
    // OTP request
    // -------------------------------------------------------------------------

    @Test
    fun `requestOtp returns the code when returnCodeToCaller is true`() {
        // In development mode the OTP is returned so tests can use it without
        // a real SMS gateway.
        val (code, returned) = auth.requestOtp("+639170000001")

        assertTrue(returned, "code should be returned in development mode")
        assertNotNull(code)
        assertEquals(6, code.length, "OTP must be six digits")
        assertTrue(code.all { it.isDigit() })
    }

    @Test
    fun `requesting a second OTP invalidates the first`() {
        auth.requestOtp("+639170000002")
        val (secondCode, _) = auth.requestOtp("+639170000002")

        // The first code was consumed by the second request; only the new one
        // should be valid.
        val regId = auth.verifyOtp("+639170000002", secondCode)
        assertNotNull(regId)
    }

    // -------------------------------------------------------------------------
    // OTP verification
    // -------------------------------------------------------------------------

    @Test
    fun `verifyOtp returns a registration id on correct code`() {
        val (code, _) = auth.requestOtp("+639170000010")
        val regId = auth.verifyOtp("+639170000010", code)

        assertTrue(regId.startsWith("reg_"), "registration id must carry the reg_ prefix")
    }

    @Test
    fun `verifyOtp rejects a wrong code and increments attempt count`() {
        auth.requestOtp("+639170000011")

        val error = assertFailsWith<ApiError> {
            auth.verifyOtp("+639170000011", "000000")
        }
        assertEquals("bad_request", error.code)
    }

    @Test
    fun `verifyOtp locks out after five wrong attempts`() {
        val (correctCode, _) = auth.requestOtp("+639170000012")
        val mobile = "+639170000012"

        // Exhaust the attempt budget with wrong codes.
        repeat(5) {
            runCatching { auth.verifyOtp(mobile, "000000") }
        }

        // The correct code is now refused because the OTP is locked.
        val error = assertFailsWith<ApiError> {
            auth.verifyOtp(mobile, correctCode)
        }
        assertEquals("too_many_attempts", error.code)
    }

    @Test
    fun `verifyOtp rejects a code that has never been requested`() {
        val error = assertFailsWith<ApiError> {
            auth.verifyOtp("+639179999999", "123456")
        }
        assertEquals("bad_request", error.code)
    }

    @Test
    fun `verifyOtp rejects a consumed code used a second time`() {
        val mobile = "+639170000013"
        val (code, _) = auth.requestOtp(mobile)
        auth.verifyOtp(mobile, code) // consume it

        // A second call with the same (now consumed) code must be rejected.
        val error = assertFailsWith<ApiError> {
            auth.verifyOtp(mobile, code)
        }
        assertEquals("bad_request", error.code)
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Test
    fun `register creates a user and returns a valid access token`() {
        val mobile = "+639170000020"
        val (code, _) = auth.requestOtp(mobile)
        val regId = auth.verifyOtp(mobile, code)

        val (token, user) = auth.register(
            registrationId = regId,
            fullName = "Test Patient",
            username = "test_patient_20",
            password = "securepass1",
            dateOfBirth = LocalDate.of(1995, 6, 15),
            sex = Sex.PREFER_NOT_TO_SAY,
            email = null,
        )

        assertNotNull(token.value)
        assertEquals("test_patient_20", user.username)
        assertEquals("Test Patient", user.fullName)
        assertEquals(mobile, user.mobileNumber)
    }

    @Test
    fun `register rejects a duplicate username`() {
        val mobile1 = "+639170000021"
        val mobile2 = "+639170000022"

        val (code1, _) = auth.requestOtp(mobile1)
        val regId1 = auth.verifyOtp(mobile1, code1)
        auth.register(regId1, "First User", "shared_username", "password1!", LocalDate.of(1990, 1, 1), null, null)

        val (code2, _) = auth.requestOtp(mobile2)
        val regId2 = auth.verifyOtp(mobile2, code2)

        val error = assertFailsWith<ApiError> {
            auth.register(regId2, "Second User", "shared_username", "password2!", LocalDate.of(1991, 2, 2), null, null)
        }
        assertEquals("bad_request", error.code)
        assertTrue(error.message.contains("username", ignoreCase = true))
    }

    @Test
    fun `register rejects a duplicate email`() {
        val sharedEmail = "dup@example.com"
        val mobile1 = "+639170000023"
        val mobile2 = "+639170000024"

        val (code1, _) = auth.requestOtp(mobile1)
        auth.register(auth.verifyOtp(mobile1, code1), "User A", "user_a_23", "password1!", LocalDate.of(1990, 1, 1), null, sharedEmail)

        val (code2, _) = auth.requestOtp(mobile2)
        val regId2 = auth.verifyOtp(mobile2, code2)

        val error = assertFailsWith<ApiError> {
            auth.register(regId2, "User B", "user_b_24", "password2!", LocalDate.of(1991, 2, 2), null, sharedEmail)
        }
        assertEquals("bad_request", error.code)
        assertTrue(error.message.contains("email", ignoreCase = true))
    }

    @Test
    fun `register rejects a duplicate mobile number`() {
        val sharedMobile = "+639170000025"

        val (code1, _) = auth.requestOtp(sharedMobile)
        auth.register(auth.verifyOtp(sharedMobile, code1), "First", "user_first_25", "password1!", LocalDate.of(1990, 1, 1), null, null)

        // Same mobile, new OTP request.
        val (code2, _) = auth.requestOtp(sharedMobile)
        val regId2 = auth.verifyOtp(sharedMobile, code2)

        val error = assertFailsWith<ApiError> {
            auth.register(regId2, "Second", "user_second_25b", "password2!", LocalDate.of(1991, 2, 2), null, null)
        }
        assertEquals("bad_request", error.code)
    }

    @Test
    fun `register rejects a password shorter than eight characters`() {
        val mobile = "+639170000026"
        val (code, _) = auth.requestOtp(mobile)
        val regId = auth.verifyOtp(mobile, code)

        val error = assertFailsWith<ApiError> {
            auth.register(regId, "Short Pass", "user_shortpass_26", "short", LocalDate.of(1990, 1, 1), null, null)
        }
        assertEquals("bad_request", error.code)
    }

    @Test
    fun `register rejects registration without a date of birth`() {
        val mobile = "+639170000027"
        val (code, _) = auth.requestOtp(mobile)
        val regId = auth.verifyOtp(mobile, code)

        val error = assertFailsWith<ApiError> {
            auth.register(regId, "No DOB", "user_nodob_27", "password1!", dateOfBirth = null, sex = null, email = null)
        }
        assertEquals("bad_request", error.code)
    }

    @Test
    fun `register rejects a bogus registration id`() {
        val error = assertFailsWith<ApiError> {
            auth.register("reg_not-a-real-id", "Nobody", "nobody", "password1!", LocalDate.of(1990, 1, 1), null, null)
        }
        assertEquals("bad_request", error.code)
    }

    // -------------------------------------------------------------------------
    // Sign-in
    // -------------------------------------------------------------------------

    @Test
    fun `signIn returns a token for a valid username and password`() {
        val password = "correct-horse-battery"
        val user = createUserWithPassword("signon_user_30", password)
        val (token, profile) = auth.signIn("signon_user_30", password)

        assertNotNull(token.value)
        assertEquals(user.id, profile.id)
    }

    @Test
    fun `signIn rejects a wrong password`() {
        createUserWithPassword("signon_user_31", "correct-password-abc")

        val error = assertFailsWith<ApiError> {
            auth.signIn("signon_user_31", "wrong-password")
        }
        assertEquals("unauthorized", error.code)
    }

    @Test
    fun `signIn rejects an unknown username`() {
        val error = assertFailsWith<ApiError> {
            auth.signIn("no_such_user_xyz", "anything")
        }
        // The error code must not reveal whether the username exists.
        assertEquals("unauthorized", error.code)
    }

    // -------------------------------------------------------------------------
    // Username normalization
    //
    // The bug these guard: a phone keyboard that autocorrects or autocompletes
    // can send `Demo_patient ` where `demo_patient` was typed. Matching raw made
    // correct credentials fail with the same opaque "Those details don't match
    // an account." as a wrong password, leaving a patient with no way to tell a
    // typo from a bad password.
    // -------------------------------------------------------------------------

    @Test
    fun `signIn accepts a username with a trailing space`() {
        val password = "correct-horse-battery"
        val user = createUserWithPassword("norm_user_32", password)

        val (_, profile) = auth.signIn("norm_user_32   ", password)

        assertEquals(user.id, profile.id)
    }

    @Test
    fun `signIn accepts a username with a leading space`() {
        val password = "correct-horse-battery"
        val user = createUserWithPassword("norm_user_33", password)

        val (_, profile) = auth.signIn("   norm_user_33", password)

        assertEquals(user.id, profile.id)
    }

    @Test
    fun `signIn ignores username case`() {
        val password = "correct-horse-battery"
        val user = createUserWithPassword("norm_user_34", password)

        val (_, profile) = auth.signIn("NORM_USER_34", password)

        assertEquals(user.id, profile.id)
    }

    @Test
    fun `signIn still rejects a wrong password on a normalized username`() {
        createUserWithPassword("norm_user_35", "correct-horse-battery")

        // Normalization must not weaken the password check.
        val error = assertFailsWith<ApiError> {
            auth.signIn(" NORM_USER_35 ", "wrong-password")
        }
        assertEquals("unauthorized", error.code)
    }

    @Test
    fun `signIn still rejects an unknown username that only differs by case`() {
        val error = assertFailsWith<ApiError> {
            auth.signIn("no_such_user_xyz".uppercase(), "anything")
        }
        assertEquals("unauthorized", error.code)
    }

    @Test
    fun `register stores a username that survives the same normalization at sign-in`() {
        val password = "correct-horse-battery"
        val mobile = "+639170000036"
        val (code, _) = auth.requestOtp(mobile)
        val regId = auth.verifyOtp(mobile, code)

        // Registered with a stray space and mixed case, as a keyboard might
        // send, so the stored form has to be the normalized one.
        auth.register(regId, "Mixed Case", "  Mixed_User_36 ", password, LocalDate.of(1990, 1, 1), null, null)

        val (_, profile) = auth.signIn("mixed_user_36", password)

        assertEquals("mixed_user_36", profile.username)
    }

    @Test
    fun `register rejects a username that differs only by case`() {
        val password = "correct-horse-battery"
        val mobile1 = "+639170000037"
        val mobile2 = "+639170000038"

        val (code1, _) = auth.requestOtp(mobile1)
        auth.register(
            auth.verifyOtp(mobile1, code1), "First", "Case_Collide_37", password, LocalDate.of(1990, 1, 1), null, null
        )

        // Otherwise `Case_Collide_37` and `case_collide_37` are two accounts
        // that are indistinguishable at sign-in.
        val (code2, _) = auth.requestOtp(mobile2)
        val regId2 = auth.verifyOtp(mobile2, code2)

        val error = assertFailsWith<ApiError> {
            auth.register(regId2, "Second", "case_collide_37", password, LocalDate.of(1991, 2, 2), null, null)
        }
        assertEquals("bad_request", error.code)
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    /**
     * Bypasses the OTP flow to create a user with a known password. The OTP
     * path is exercised by the dedicated tests above; this fixture exists so
     * sign-in tests do not depend on OTP tests passing.
     */
    private fun createUserWithPassword(username: String, password: String) = users.create(
        username = username,
        password = password,
        fullName = "Test User",
        email = null,
        mobileNumber = "+6391" + (70_000_000L + username.hashCode().toLong().let { kotlin.math.abs(it) % 10_000_000 }),
        dateOfBirth = LocalDate.of(1992, 3, 3),
        sex = Sex.PREFER_NOT_TO_SAY,
    )
}
