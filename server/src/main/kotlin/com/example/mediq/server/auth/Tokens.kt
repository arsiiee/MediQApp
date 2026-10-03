package com.example.mediq.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.time.Instant
import java.util.Date
import java.util.UUID

data class TokenClaims(
    val sessionId: String,
    val userId: String,
    val role: String,
)

/**
 * Issues and checks the access token.
 *
 * The token carries a session id rather than standing on its own. A stateless
 * JWT stays valid until it expires no matter what the app does, so signing out
 * would be cosmetic — the route layer checks [com.example.mediq.server.db.Sessions]
 * on every request, and a revoked session rejects its token immediately.
 */
class Tokens(private val secret: String, private val issuer: String) {

    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    /**
     * Explicit algorithm allow-list: without it, a token signed with `alg: none`
     * is accepted.
     *
     * Exposed so Ktor's `jwt { verifier(...) }` uses exactly this verifier
     * rather than building a second one that could drift from it.
     */
    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .build()

    fun issue(sessionId: String, userId: String, role: String, expiresAt: Instant): String =
        JWT.create()
            .withIssuer(issuer)
            .withSubject(userId)
            .withClaim("sid", sessionId)
            .withClaim("role", role)
            .withExpiresAt(Date.from(expiresAt))
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)

    /** Returns null for any token that is malformed, unsigned, wrong issuer, or expired. */
    fun verify(token: String): TokenClaims? = try {
        val decoded = verifier.verify(token)
        val sid = decoded.getClaim("sid").asString()
        val role = decoded.getClaim("role").asString()
        if (sid.isNullOrBlank() || role.isNullOrBlank()) {
            null
        } else {
            TokenClaims(sessionId = sid, userId = decoded.subject ?: "", role = role)
        }
    } catch (e: JWTVerificationException) {
        null
    }
}