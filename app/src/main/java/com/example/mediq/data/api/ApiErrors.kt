package com.example.mediq.data.api

import com.example.mediq.domain.model.ApiFailure
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

/**
 * Translates transport failures into [ApiFailure], the one error type the UI
 * layer sees.
 *
 * The server commits to a readable error body on every non-2xx response —
 * `ErrorDto(error, message)` from `configureStatusPages`, written by
 * `ApiError` for a patient to read. This file is the client half of that
 * contract: it reads the body and hands the message up unchanged.
 *
 * Without it, `Retrofit` throws `HttpException`, whose `message` is the string
 * `"HTTP 401 "`. Every ViewModel that surfaced `e.message` therefore showed a
 * patient their own HTTP status instead of the sentence the server wrote for
 * them.
 *
 * [call] is the single entry point. Wrapping a repository call in it means the
 * mapping cannot be forgotten at an individual call site, and a failure that
 * escapes a repository is always an [ApiFailure] with a message safe to show.
 */
suspend fun <T> call(block: suspend () -> T): T = try {
    block()
} catch (e: HttpException) {
    throw e.toApiFailure()
} catch (e: IOException) {
    // No response at all: server down, no route to host, timeout, TLS failure.
    // The message never reaches a screen — list reads branch on
    // `isNetworkFailure` and single reads use this copy as-is.
    throw ApiFailure(
        status = ApiFailure.STATUS_NO_RESPONSE,
        code = ApiFailure.CODE_NETWORK,
        message = "Couldn't reach the clinic. Check your connection and try again.",
        cause = e,
    )
}

/**
 * Reads the server's error body off [this] and returns it as an [ApiFailure].
 *
 * The body is one-shot: `errorBody()` can only be consumed once, so it is read
 * exactly once here and never re-read.
 *
 * A body that will not parse — an HTML page from a proxy, a truncated body, an
 * empty one — is not a crash. It degrades to a generic message keyed by status,
 * because a patient needs "Please sign in again." far more than they need to
 * know their gateway mangled the response.
 */
private fun HttpException.toApiFailure(): ApiFailure {
    val status = code()
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()

    val parsed = body
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { Gson().fromJson(it, ErrorDto::class.java) }.getOrNull() }

    val code = parsed?.error?.takeIf { it.isNotBlank() }
    val message = parsed?.message?.takeIf { it.isNotBlank() }

    return if (code != null && message != null) {
        ApiFailure(status = status, code = code, message = message)
    } else {
        ApiFailure(status = status, code = ApiFailure.CODE_MALFORMED, message = messageFor(status))
    }
}

/**
 * Last-resort copy for a failure whose body could not be read.
 *
 * 401 and 403 get their own wording because an expired token is the single most
 * likely reason a patient sees this, and "Please sign in again." tells them what
 * to do. Everything else gets the same sentence the server uses for a 500.
 */
private fun messageFor(status: Int): String = when (status) {
    401, 403 -> "Please sign in again."
    else -> ApiFailure.MESSAGE_UNEXPECTED
}
