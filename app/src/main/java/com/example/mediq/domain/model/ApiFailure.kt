package com.example.mediq.domain.model

/**
 * A request that failed, in terms the UI can act on.
 *
 * The server already writes every error for a patient to read — see
 * `http/ErrorDto` and the `ApiError` factory methods. This type is where those
 * messages stop being a transport concern and become something a ViewModel can
 * put on screen unchanged.
 *
 * It exists because the alternative was showing raw framework text. Retrofit's
 * [retrofit2.HttpException] reports `"HTTP 401 "` as its message, so a patient
 * who typed the wrong password was told "HTTP 401". Nothing in `data/` decided
 * that; the error body was simply never read.
 *
 * [status] is the HTTP status, or [STATUS_NO_RESPONSE] when the request never
 * reached the server. [code] is the server's machine-readable code
 * (`slot_taken`, `too_many_attempts`, …) or [CODE_NETWORK] / [CODE_MALFORMED]
 * when there was no body to read a code from.
 *
 * Plain Kotlin with no Android imports: `:server` compiles this package as
 * `sharedDomain`.
 */
class ApiFailure(
    val status: Int,
    val code: String,
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * True when the request never reached the server.
     *
     * List reads use this to show an empty state instead of an error, matching
     * the rule in `AGENTS.md` that an unreachable backend is the expected state
     * during development rather than a failure.
     */
    val isNetworkFailure: Boolean get() = code == CODE_NETWORK

    override fun toString(): String = "ApiFailure(status=$status, code=$code)"

    companion object {
        /** No HTTP status: the connection failed, so there was no response. */
        const val STATUS_NO_RESPONSE = 0

        const val CODE_NETWORK = "network_unreachable"

        /** A non-2xx response arrived, but its body was not a readable [ErrorDto]. */
        const val CODE_MALFORMED = "malformed_error_response"

        /** Fallback for a server-side failure, worded the way the server words it. */
        const val MESSAGE_UNEXPECTED = "Something went wrong. Please try again."
    }
}
