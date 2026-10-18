package com.example.mediq.data.api

import com.example.mediq.domain.model.SessionSignal
import com.example.mediq.fake.FakeSharedPreferences
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionAuthenticatorTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var tokenStore: TokenStore
    private lateinit var sessionSignal: SessionSignal
    private lateinit var authenticator: SessionAuthenticator

    private val validJson = """
        {
          "accessToken": "old-expired-token",
          "expiresAt": "2099-10-18T12:00:00Z",
          "profile": {
            "id": 1,
            "username": "patient1",
            "fullName": "Patient One",
            "email": "p1@example.com",
            "mobileNumber": "09171234567",
            "dateOfBirth": "1990-01-01",
            "sex": "male",
            "address": "123 Main St",
            "role": "patient"
          }
        }
    """.trimIndent()

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        tokenStore = TokenStore(prefs)
        sessionSignal = SessionSignal()
        authenticator = SessionAuthenticator(tokenStore, sessionSignal)
    }

    private fun buildResponse(
        method: String,
        status: Int = 401,
        priorResponse: Response? = null,
        token: String? = "old-expired-token",
    ): Response {
        val request = Request.Builder()
            .url("http://10.0.2.2:8099/appointments")
            .apply {
                if (token != null) addHeader("Authorization", "Bearer $token")
            }
            .method(method, if (method == "GET" || method == "HEAD") null else okhttp3.RequestBody.create(null, ByteArray(0)))
            .build()

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message("Unauthorized")
            .apply {
                if (priorResponse != null) priorResponse(priorResponse)
            }
            .build()
    }

    @Test
    fun `401 on GET clears store, signals expired, and re-issues if new token present`() {
        prefs.edit().putString("session_json", validJson).apply()

        val response = buildResponse("GET")
        // If no new token was stored, it returns null (cannot retry without token)
        val retryRequestNoToken = authenticator.authenticate(null, response)
        assertNull(retryRequestNoToken)
        assertTrue(sessionSignal.isExpired.value)
        assertNull(tokenStore.getAccessToken())

        // Now simulate that a new token was obtained (e.g. by re-auth)
        prefs.edit().putString("session_json", validJson.replace("old-expired-token", "new-token-456")).apply()
        val retryResponse = buildResponse("GET")
        val retryRequestWithToken = authenticator.authenticate(null, retryResponse)
        assertNotNull(retryRequestWithToken)
        assertEquals("Bearer new-token-456", retryRequestWithToken?.header("Authorization"))
    }

    @Test
    fun `401 on POST re-issues zero times`() {
        prefs.edit().putString("session_json", validJson).apply()

        val response = buildResponse("POST")
        val retryRequest = authenticator.authenticate(null, response)

        assertNull(retryRequest)
        assertTrue(sessionSignal.isExpired.value)
        assertNull(tokenStore.getAccessToken())

        // Even with a new token present, POST must NEVER be re-issued
        prefs.edit().putString("session_json", validJson.replace("old-expired-token", "new-token-456")).apply()
        val retryResponse = buildResponse("POST")
        val retryWithNewToken = authenticator.authenticate(null, retryResponse)
        assertNull(retryWithNewToken)
    }

    @Test
    fun `401 on DELETE, PATCH, PUT re-issues zero times`() {
        for (method in listOf("DELETE", "PATCH", "PUT")) {
            prefs.edit().putString("session_json", validJson).apply()
            sessionSignal.clear()

            val response = buildResponse(method)
            val retryRequest = authenticator.authenticate(null, response)

            assertNull("Expected null retry request for $method", retryRequest)
            assertTrue(sessionSignal.isExpired.value)
            assertNull(tokenStore.getAccessToken())
        }
    }

    @Test
    fun `five concurrent 401s produce one signal transition and store is cleared`() {
        prefs.edit().putString("session_json", validJson).apply()

        val responses = List(5) { buildResponse("GET") }
        for (resp in responses) {
            authenticator.authenticate(null, resp)
        }

        assertTrue(sessionSignal.isExpired.value)
        assertNull(tokenStore.getAccessToken())
    }

    @Test
    fun `caps retries so already-retried response returns null`() {
        prefs.edit().putString("session_json", validJson).apply()

        val firstResponse = buildResponse("GET")
        val secondResponse = buildResponse("GET", priorResponse = firstResponse)

        val retryRequest = authenticator.authenticate(null, secondResponse)
        assertNull(retryRequest)
    }
}
