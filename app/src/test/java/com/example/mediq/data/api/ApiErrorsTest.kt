package com.example.mediq.data.api

import com.example.mediq.domain.model.ApiFailure
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Proves the client reads the error contract the server publishes.
 *
 * The bug this guards: `HttpException.message` is `"HTTP 401 "`, and the client
 * used to hand that straight to a ViewModel. So a patient who typed the wrong
 * password saw "HTTP 401" rather than the sentence `AuthService` wrote for
 * them. Nothing about that failure was visible in a test — the server was
 * behaving perfectly the whole time.
 */
class ApiErrorsTest {

    private val json = "application/json".toMediaType()

    /** Builds the exception Retrofit throws for a non-2xx response. */
    private fun httpError(status: Int, body: String): HttpException =
        HttpException(Response.error<Any>(status, body.toResponseBody(json)))

    /**
     * Runs [block], expecting it to fail, and returns that failure as an
     * [ApiFailure].
     *
     * `Assert.fail` returns `Unit` rather than `Nothing`, so `?:` does not
     * smart-cast — hence the explicit cast.
     */
    private fun failureFrom(block: suspend () -> Any): ApiFailure {
        val thrown = runCatching { runBlocking { block() } }.exceptionOrNull()
        assertTrue("expected a failure, got $thrown", thrown != null)
        return thrown as ApiFailure
    }

    // --- The server's real error body is parsed -----------------------------

    @Test
    fun `401 surfaces the server message, not the HTTP status`() {
        val failure = failureFrom {
            call { throw httpError(401, """{"error":"unauthorized","message":"Those details don't match an account."}""") }
        }

        assertEquals("Those details don't match an account.", failure.message)
        assertEquals("unauthorized", failure.code)
        assertEquals(401, failure.status)
    }

    @Test
    fun `409 slot_taken keeps the message that tells the patient what to do`() {
        val failure = failureFrom {
            call {
                throw httpError(409, """{"error":"slot_taken","message":"That time was just taken. Please pick another."}""")
            }
        }

        assertEquals("That time was just taken. Please pick another.", failure.message)
        assertEquals("slot_taken", failure.code)
    }

    @Test
    fun `the raw HttpException message is never what reaches the UI`() {
        val raw = httpError(401, """{"error":"unauthorized","message":"Those details don't match an account."}""")

        // The behaviour this whole change exists to prevent. Retrofit builds
        // this as "HTTP <code> <reason phrase>", so the tail depends on the
        // response — only the prefix is stable, and only the prefix matters:
        // it is what a patient saw instead of a sentence.
        assertTrue(
            "expected an HTTP status string, got '${raw.message}'",
            raw.message.orEmpty().startsWith("HTTP 401"),
        )

        val failure = failureFrom { call { throw raw } }
        assertFalse(failure.message.contains("HTTP"))
        assertEquals("Those details don't match an account.", failure.message)
    }

    // --- A body that is not the contract must not crash ----------------------

    @Test
    fun `an HTML error page degrades to a message instead of throwing`() {
        val failure = failureFrom {
            call { throw httpError(500, "<html><body>502 Bad Gateway</body></html>") }
        }

        assertEquals(ApiFailure.CODE_MALFORMED, failure.code)
        assertEquals(ApiFailure.MESSAGE_UNEXPECTED, failure.message)
    }

    @Test
    fun `an empty body degrades rather than surfacing a null message`() {
        val failure = failureFrom { call { throw httpError(500, "") } }

        assertEquals(ApiFailure.CODE_MALFORMED, failure.code)
        assertTrue(failure.message.isNotBlank())
    }

    @Test
    fun `a body missing the message field does not surface a null message`() {
        val failure = failureFrom { call { throw httpError(400, """{"error":"bad_request"}""") } }

        assertEquals(ApiFailure.CODE_MALFORMED, failure.code)
        assertTrue(failure.message.isNotBlank())
    }

    @Test
    fun `401 with an unreadable body still tells the patient to sign in again`() {
        val failure = failureFrom { call { throw httpError(401, "nonsense") } }

        assertEquals("Please sign in again.", failure.message)
    }

    // --- No response at all --------------------------------------------------

    @Test
    fun `an unreachable server is flagged as a network failure`() {
        val failure = failureFrom { call { throw IOException("Failed to connect to 10.0.2.2:8099") } }

        assertEquals(ApiFailure.CODE_NETWORK, failure.code)
        assertEquals(ApiFailure.STATUS_NO_RESPONSE, failure.status)
        assertTrue(failure.isNetworkFailure)
    }

    @Test
    fun `a network failure message never leaks the host or port`() {
        val failure = failureFrom { call { throw IOException("Failed to connect to 10.0.2.2:8099") } }

        assertFalse(failure.message.contains("10.0.2.2"))
        assertFalse(failure.message.contains("8099"))
    }

    // --- Success must pass through untouched ---------------------------------

    @Test
    fun `a successful call returns its value and does not wrap it`() {
        val result = runBlocking { call { "a doctor" } }
        assertEquals("a doctor", result)
    }

    @Test
    fun `an ApiFailure is not double-wrapped when it passes through`() {
        val original = ApiFailure(409, "slot_taken", "That time was just taken.")
        val rethrown = runCatching { runBlocking { call { throw original } } }.exceptionOrNull()

        assertTrue("expected the original instance back", rethrown === original)
    }
}
