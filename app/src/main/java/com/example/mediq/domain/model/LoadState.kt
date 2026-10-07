package com.example.mediq.domain.model

/**
 * The three things a screen can be showing for any request.
 *
 * Every screen handles all three, so adding a backend later doesn't mean
 * rewriting the screens — only filling in what they already draw.
 */
sealed interface LoadState<out T> {

    /** Waiting on the answer. */
    data object Loading : LoadState<Nothing>

    /** The answer arrived. [data] may legitimately be empty. */
    data class Success<T>(val data: T) : LoadState<T>

    /** The request failed. [message] is safe to show to a user. */
    data class Error(val message: String) : LoadState<Nothing>
}

/** Convenience for the common case of a list. */
val <T> LoadState<List<T>>.dataOrEmpty: List<T>
    get() = (this as? LoadState.Success)?.data.orEmpty()

val <T> LoadState<T>.isLoading: Boolean
    get() = this is LoadState.Loading