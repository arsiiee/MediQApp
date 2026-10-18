package com.example.mediq.data.api

import com.example.mediq.domain.model.AuthSession
import com.google.gson.Gson

/**
 * Pure mapping and validation of persisted session JSON strings into [AuthSession].
 *
 * Before this existed, `getSession()` wrapped only the Gson parse, so a malformed
 * or corrupt `expiresAt` or `dateOfBirth` string survived the parse and threw
 * `DateTimeParseException` later at [toDomain] outside of any `runCatching` guard.
 * This left the Profile screen wedged in `LoadState.Error` indefinitely with the
 * corrupt session token still stored.
 *
 * By isolating validation into a pure function over raw JSON strings, mapping
 * and parsing exceptions are caught at the storage boundary without requiring
 * Android Context or SharedPreferences in unit tests.
 */
object SessionMapper {

    private val gson = Gson()

    /**
     * Maps [json] to an [AuthSessionDto], returning null if parsing fails.
     */
    fun parseDto(json: String?): AuthSessionDto? {
        if (json.isNullOrBlank()) return null
        return runCatching { gson.fromJson(json, AuthSessionDto::class.java) }.getOrNull()
    }

    /**
     * Maps [json] to a domain [AuthSession].
     *
     * Returns null if [json] is null/blank, malformed JSON, or contains fields
     * that cannot be converted to valid domain objects (such as unparseable
     * timestamps or dates).
     */
    fun toDomain(json: String?): AuthSession? {
        val dto = parseDto(json) ?: return null
        return runCatching { dto.toDomain() }.getOrNull()
    }
}
