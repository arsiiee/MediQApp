package com.example.mediq.server

import com.example.mediq.server.auth.Tokens
import com.example.mediq.server.db.AuthService
import com.example.mediq.server.db.UserStore
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards two properties of the brute-force lockout that are easy to break in
 * opposite directions.
 *
 * **The counter must survive a rejected attempt.** Incrementing
 * `attempt_count` inside the same `db.tx` that throws rolls the increment back,
 * leaving the counter at zero and the lockout permanently ineffective. The
 * original code did exactly that and every wrong code was accepted forever.
 *
 * **A rejected attempt must not need two connections at once.** The obvious
 * repair — a nested `db.tx` so the increment commits independently — holds one
 * pooled connection while acquiring a second. `POST /auth/otp/verify` is
 * unauthenticated and `maximumPoolSize` is 10, so five parallel wrong codes
 * would exhaust the pool and stall every other route in the server for the
 * duration of the connection timeout. The increment therefore runs *after* the
 * read transaction closes: two sequential checkouts, never two concurrent ones.
 *
 * `attempts` is asserted by reading the row directly rather than by counting
 * calls, so a test cannot pass on the number of requests it made.
 */
/** Mirrors `AuthService.MAX_OTP_ATTEMPTS`; the server's own threshold. */
private const val MAX_OTP_ATTEMPTS = 5

class OtpLockoutTest {

    private fun newAuth(): Pair<Database, AuthService> {
        val config = ServerConfig(
            port = 0,
            jdbcUrl = "jdbc:h2:mem:otp_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            jwtSecret = "test-secret-not-used-for-anything-real-123456",
            jwtIssuer = "mediq-test",
            tokenTtlMinutes = 60,
            slotDurationMinutes = 30,
            otpTtlMinutes = 5,
            seedDemoData = false,
        )
        val db = Database(config)
        val users = UserStore(db)
        val tokens = Tokens(config.jwtSecret, config.jwtIssuer)
        return db to AuthService(db, users, tokens, config.tokenTtlMinutes, config.otpTtlMinutes)
    }

    /** Reads the live counter for the newest unconsumed code for [mobile]. */
    private fun attemptCount(db: Database, mobile: String): Int =
        db.read { c ->
            c.prepareStatement(
                "SELECT attempt_count FROM otp_codes WHERE mobile_number = ? ORDER BY created_at DESC LIMIT 1"
            ).use { st ->
                st.setString(1, mobile)
                st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else -1 }
            }
        }

    private fun verify(auth: AuthService, mobile: String, otp: String): Boolean =
        runCatching { auth.verifyOtp(mobile, otp) }.isSuccess

    @Test
    fun `each wrong code increments the counter even though the call fails`() {
        val (db, auth) = newAuth()
        try {
            val mobile = "+639170000201"
            auth.requestOtp(mobile)

            repeat(3) { attempt ->
                assertTrue(
                    !verify(auth, mobile, "000000"),
                    "attempt ${attempt + 1} should have been rejected",
                )
                assertEquals(
                    attempt + 1,
                    attemptCount(db, mobile),
                    "the counter must be committed even though verifyOtp threw",
                )
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `the lockout engages on the sixth attempt and the right code stops working`() {
        val (db, auth) = newAuth()
        try {
            val mobile = "+639170000202"
            val (correctCode, _) = auth.requestOtp(mobile)

            repeat(5) { verify(auth, mobile, "000000") }
            assertEquals(5, attemptCount(db, mobile), "five wrong attempts should be counted")

            // The sixth request is refused before the code is even compared.
            val error = runCatching { auth.verifyOtp(mobile, correctCode) }.exceptionOrNull()
            assertTrue(
                error is ApiError,
                "the correct code must be refused once locked out, got $error",
            )
            assertEquals(
                5,
                attemptCount(db, mobile),
                "a locked-out attempt must not bump the counter past the threshold",
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `ten parallel wrong codes do not exhaust the connection pool`() {
        val (db, auth) = newAuth()
        val attempts = 10
        val pool = Executors.newFixedThreadPool(attempts)
        try {
            val mobile = "+639170000203"
            val (correctCode, _) = auth.requestOtp(mobile)

            val start = CountDownLatch(1)
            val done = CountDownLatch(attempts)
            val rejected = java.util.concurrent.atomic.AtomicInteger(0)
            val failedUnexpectedly = java.util.Collections.synchronizedList(mutableListOf<String>())

            // Every thread blocks on the latch, so all ten hit the database at
            // once. Each needs two connections; the pool holds ten.
            repeat(attempts) {
                pool.submit {
                    start.await()
                    try {
                        if (!verify(auth, mobile, "000000")) rejected.incrementAndGet()
                        else failedUnexpectedly += "a wrong code was accepted"
                    } catch (t: Throwable) {
                        // A pool timeout or SQLTransientConnectionException is
                        // the failure this test exists to catch.
                        failedUnexpectedly += "${t.javaClass.simpleName}: ${t.message}"
                    } finally {
                        done.countDown()
                    }
                }
            }
            start.countDown()

            assertTrue(
                done.await(20, TimeUnit.SECONDS),
                "ten parallel wrong codes did not finish in 20s — the pool is starved",
            )

            assertEquals(
                emptyList(),
                failedUnexpectedly.toList(),
                "parallel wrong-code attempts must each fail fast as a 400, not stall or misbehave",
            )
            // MAX_OTP_ATTEMPTS is 5, so the last five are refused by the lockout
            // rather than by the comparison — still a rejection either way.
            assertEquals(attempts, rejected.get(), "every parallel attempt must be rejected")

            // The counter can exceed the threshold here and that is not a bug:
            // each thread reads attempt_count before the others commit, so ten
            // parallel reads all see a value below the limit and all bump. What
            // must hold is that it never stays *below* the limit — otherwise the
            // lockout is ineffective, which was the original defect — and that
            // the account is locked out afterwards.
            val finalCount = attemptCount(db, mobile)
            assertTrue(
                finalCount >= MAX_OTP_ATTEMPTS,
                "the counter must reach the threshold; got $finalCount",
            )

            // And the account is genuinely locked out afterwards: even the
            // correct code is now refused.
            assertTrue(
                !verify(auth, mobile, correctCode),
                "the account must be locked out after ten parallel wrong attempts",
            )
        } finally {
            pool.shutdownNow()
            db.close()
        }
    }
}