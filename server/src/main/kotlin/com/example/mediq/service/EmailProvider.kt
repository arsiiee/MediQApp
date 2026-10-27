package com.example.mediq.service

import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Sends a one-time verification code to an email address.
 *
 * The provider is swapped at wiring time: [ResendEmailProvider] when a
 * `RESEND_API_KEY` is configured, [LoggingEmailProvider] locally so nothing
 * is actually sent and no key is ever logged.
 */
interface EmailProvider {
    /** Sends [body] with [subject] to [to]. Never logs the API key. */
    suspend fun send(to: String, subject: String, body: String)
}

/**
 * Configuration for the Resend email provider.
 *
 * [from] is the sender address; it must be a verified domain/sender in Resend.
 * [baseUrl] defaults to the Resend API and can be overridden for testing.
 */
data class EmailConfig(
    val apiKey: String,
    val from: String = "MediQ <onboarding@resend.dev>",
    val baseUrl: String = "https://api.resend.com",
)

@Serializable
data class ResendEmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val html: String,
)

/**
 * Sends verification emails via Resend.
 *
 * The API key is read from the environment at construction time and is never
 * written to a log. Email bodies (which may contain verification codes) are
 * also never logged.
 */
class ResendEmailProvider(
    private val httpClient: HttpClient,
    private val config: EmailConfig,
) : EmailProvider {
    private val logger = LoggerFactory.getLogger(ResendEmailProvider::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun send(to: String, subject: String, body: String) {
        val request = ResendEmailRequest(
            from = config.from,
            to = listOf(to),
            subject = subject,
            html = body,
        )
        val bodyStr = json.encodeToString(request)

        val response = httpClient.post("${config.baseUrl}/emails") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            setBody(bodyStr)
        }

        if (response.status.value !in 200..299) {
            // Never log the API key or email body on error.
            logger.error("Resend email failed to $to with status ${response.status}")
            throw IllegalStateException("Email delivery failed with status ${response.status}")
        }

        logger.info("Verification email sent via Resend to $to: $subject")
    }
}

/**
 * Local stub: logs that an email would be sent, but never sends and never
 * logs the API key. Used when no `RESEND_API_KEY` is configured.
 */
class LoggingEmailProvider : EmailProvider {
    private val logger = LoggerFactory.getLogger(LoggingEmailProvider::class.java)

    override suspend fun send(to: String, subject: String, body: String) {
        logger.info("EmailProvider (stub): would send to $to — $subject")
    }
}
