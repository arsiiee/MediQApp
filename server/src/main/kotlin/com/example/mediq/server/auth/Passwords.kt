package com.example.mediq.server.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing with PBKDF2-HMAC-SHA256, from the JDK — no extra dependency.
 *
 * Encoded as `pbkdf2$<iterations>$<salt>$<hash>`, both Base64. The iteration
 * count is stored with the hash so it can be raised later without invalidating
 * existing passwords: verify uses whatever the stored value says, and a
 * successful login is the moment to re-hash at the new cost.
 *
 * Before real use: check the current OWASP guidance for PBKDF2-HMAC-SHA256
 * iteration counts and confirm this choice against it. Argon2id is the current
 * recommendation where memory hardness matters; this is the reasonable option
 * that needs no new dependency.
 */
object Passwords {

    private const val ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private val random = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getEncoder().withoutPadding()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val derived = pbkdf2(password, salt, ITERATIONS)
        val separator = '$'
        return listOf("pbkdf2", ITERATIONS, encoder.encodeToString(salt), encoder.encodeToString(derived))
            .joinToString(separator.toString())
    }

    fun verify(password: String, encoded: String): Boolean {
        val parts = encoded.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull() ?: return false
        val actual = pbkdf2(password, salt, iterations)
        // Constant-time, so a wrong password cannot be discovered by timing.
        return MessageDigest.isEqual(expected, actual)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}