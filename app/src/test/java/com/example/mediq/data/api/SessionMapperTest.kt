package com.example.mediq.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SessionMapperTest {

    private val validJson = """
        {
          "accessToken": "valid-token-123",
          "expiresAt": "2026-10-18T12:00:00Z",
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

    @Test
    fun `valid blob maps to domain session`() {
        val session = SessionMapper.toDomain(validJson)
        assertNotNull(session)
        assertEquals("valid-token-123", session?.accessToken)
        assertEquals("patient1", session?.profile?.username)
    }

    @Test
    fun `malformed expiresAt returns null and does not throw`() {
        val corruptJson = validJson.replace("2026-10-18T12:00:00Z", "not-a-timestamp")
        val session = SessionMapper.toDomain(corruptJson)
        assertNull(session)
    }

    @Test
    fun `malformed dateOfBirth returns null and does not throw`() {
        val corruptJson = validJson.replace("1990-01-01", "not-a-date")
        val session = SessionMapper.toDomain(corruptJson)
        assertNull(session)
    }

    @Test
    fun `empty string returns null`() {
        assertNull(SessionMapper.toDomain(""))
        assertNull(SessionMapper.toDomain("   "))
    }

    @Test
    fun `absent or null string returns null`() {
        assertNull(SessionMapper.toDomain(null))
    }

    @Test
    fun `malformed json syntax returns null`() {
        assertNull(SessionMapper.toDomain("{ not valid json }"))
    }
}
