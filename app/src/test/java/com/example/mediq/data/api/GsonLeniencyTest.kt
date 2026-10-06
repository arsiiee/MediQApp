package com.example.mediq.data.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Records how the client's JSON parser behaves against the server's wire format.
 *
 * This exists because a claim about response compatibility turned out to be
 * wrong when checked, and the reason it was wrong is not visible in the code.
 *
 * The server sets `ignoreUnknownKeys = false` in `Main.kt`. That setting governs
 * how the *server* parses an inbound request body — it does not govern what the
 * client does with a response. The client parses with Gson via
 * `GsonConverterFactory.create()`, and Gson's default is to ignore unknown
 * fields. So adding a field to a response does NOT break an older build; the
 * client silently skips it.
 *
 * The mirror image is the real risk, and it is the opposite one: Gson also
 * cannot tell "the server never sent this" from "the field was renamed". Both
 * arrive as null, with no error either way.
 */
class GsonLeniencyTest {

    private val gson = Gson()

    @Test
    fun `an unknown field in a response is ignored, not fatal`() {
        val json = """
            {"id":"d1","fullName":"Dr. Santos","yearsOfExperience":12,
             "aBrandNewFieldTheClientHasNeverHeardOf":true}
        """.trimIndent()

        val parsed = gson.fromJson(json, DoctorDto::class.java)

        assertEquals("d1", parsed.id)
        assertEquals(12, parsed.yearsOfExperience)
    }

    @Test
    fun `a missing non-null field arrives as null rather than throwing`() {
        // What happens when the server renames `fullName`. The parse succeeds.
        val json = """{"id":"d1","yearsOfExperience":12}"""

        val parsed = gson.fromJson(json, DoctorDto::class.java)

        // Declared non-null in Kotlin, actually null at runtime. Nothing threw.
        // Widened to Any? so the comparison is not the pointless one against a
        // non-null type — the silence would buy nothing over a local suppression.
        val widened: Any? = parsed.fullName
        assertNull(widened)
    }

    @Test
    fun `an absent error field does not throw when parsing an error body`() {
        val parsed = gson.fromJson("""{"message":"Wrong password."}""", ErrorDto::class.java)

        assertNotNull(parsed.message)
        assertNull(parsed.error)
    }
}
