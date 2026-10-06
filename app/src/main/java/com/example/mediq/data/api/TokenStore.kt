package com.example.mediq.data.api

import android.content.Context
import com.google.gson.Gson

/**
 * Persists the auth session (access token + profile) across app restarts
 * using SharedPreferences.
 *
 * The full [AuthSessionDto] JSON string is stored so that [currentSession()]
 * can reconstruct the complete domain [AuthSession] — including the nested
 * UserProfile — without a network call.
 *
 * Security note: the token is stored in plaintext in the app's private
 * SharedPreferences. This is sufficient for development but should be
 * replaced with EncryptedSharedPreferences (Tink) before production.
 */
class TokenStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveSession(dto: AuthSessionDto) {
        prefs.edit().putString(KEY_SESSION, gson.toJson(dto)).apply()
    }

    fun getSession(): AuthSessionDto? {
        val json = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { gson.fromJson(json, AuthSessionDto::class.java) }.getOrNull()
    }

    fun getAccessToken(): String? = getSession()?.accessToken

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    companion object {
        private const val PREFS_NAME = "mediq_auth"
        private const val KEY_SESSION = "session_json"
    }
}
