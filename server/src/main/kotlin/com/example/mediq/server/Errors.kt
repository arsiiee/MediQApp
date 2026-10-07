package com.example.mediq.server

import com.example.mediq.server.http.ErrorDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

/**
 * An error with an intended HTTP status and a message written for a patient to
 * read, not for a log file.
 *
 * Anything thrown that is not an [ApiError] is treated as a bug: it returns a
 * generic 500 and the real cause goes to the log, so an unexpected exception
 * cannot leak a table name or a stack trace to the app.
 */
class ApiError(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    companion object {
        fun badRequest(message: String) = ApiError(HttpStatusCode.BadRequest, "bad_request", message)
        fun unauthorized(message: String = "Please sign in again.") =
            ApiError(HttpStatusCode.Unauthorized, "unauthorized", message)

        fun forbidden(message: String) = ApiError(HttpStatusCode.Forbidden, "forbidden", message)
        fun notFound(message: String = "Not found.") = ApiError(HttpStatusCode.NotFound, "not_found", message)

        /** The one the booking transaction throws when the slot is already taken. */
        fun conflict(message: String) = ApiError(HttpStatusCode.Conflict, "slot_taken", message)
        fun tooManyAttempts(message: String) =
            ApiError(HttpStatusCode.TooManyRequests, "too_many_attempts", message)
    }
}

suspend fun ApplicationCall.respondApiError(error: ApiError) {
    respond(error.status, ErrorDto(error.code, error.message))
}