package com.example.mediq.data.repository

import com.example.mediq.data.api.TokenStore
import com.example.mediq.data.repository.RetrofitAuthRepository
import com.example.mediq.fake.FakeMediQApiService
import com.example.mediq.fake.FakeSharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RetrofitAuthRepositoryTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var tokenStore: TokenStore
    private lateinit var api: FakeMediQApiService
    private lateinit var repository: RetrofitAuthRepository

    private val validJson = """
        {
          "accessToken": "valid-token-123",
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
        api = FakeMediQApiService()
        repository = RetrofitAuthRepository(api, tokenStore)
    }

    @Test
    fun `currentSession returns session when valid and unexpired`() = runBlocking {
        prefs.edit().putString("session_json", validJson).apply()

        val session = repository.currentSession()
        assertNotNull(session)
        assertEquals("valid-token-123", session?.accessToken)
        assertEquals("patient1", session?.profile?.username)
    }

    @Test
    fun `currentSession returns null and clears store when expired`() = runBlocking {
        val expiredJson = validJson.replace("2099-10-18T12:00:00Z", "2000-01-01T00:00:00Z")
        prefs.edit().putString("session_json", expiredJson).apply()

        val session = repository.currentSession()
        assertNull(session)
        assertNull(prefs.getString("session_json", null))
    }

    @Test
    fun `currentSession returns null and clears store without throwing on corrupt blob`() = runBlocking {
        val corruptJson = validJson.replace("2099-10-18T12:00:00Z", "not-a-valid-timestamp")
        prefs.edit().putString("session_json", corruptJson).apply()

        val session = repository.currentSession()
        assertNull(session)
        assertNull(prefs.getString("session_json", null))
    }

    @Test
    fun `currentSession returns null when no session is stored`() = runBlocking {
        val session = repository.currentSession()
        assertNull(session)
    }

    @Test
    fun `signOut clears store and signal even when server call succeeds`() = runBlocking {
        val signal = com.example.mediq.domain.model.SessionSignal()
        signal.markExpired()
        val repoWithSignal = RetrofitAuthRepository(api, tokenStore, signal)

        prefs.edit().putString("session_json", validJson).apply()
        repoWithSignal.signOut()

        assertNull(prefs.getString("session_json", null))
        org.junit.Assert.assertFalse(signal.isExpired.value)
    }

    @Test
    fun `signOut clears store and signal even when server call throws`() = runBlocking {
        val signal = com.example.mediq.domain.model.SessionSignal()
        signal.markExpired()
        val throwingApi = object : com.example.mediq.data.api.MediQApiService by api {
            override suspend fun signOut(): com.example.mediq.data.api.AckDto {
                throw java.io.IOException("Network unreachable")
            }
        }
        val repoWithSignal = RetrofitAuthRepository(throwingApi, tokenStore, signal)

        prefs.edit().putString("session_json", validJson).apply()
        repoWithSignal.signOut()

        assertNull(prefs.getString("session_json", null))
        org.junit.Assert.assertFalse(signal.isExpired.value)
    }
}
