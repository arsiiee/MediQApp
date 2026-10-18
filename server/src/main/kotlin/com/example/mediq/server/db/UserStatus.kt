package com.example.mediq.server.db

/**
 * Whether an account can still be used.
 *
 * Server-side rather than in `domain/model/` beside
 * [com.example.mediq.domain.model.UserRole], and the reason is worth keeping:
 * that package is compiled into the app as `sharedDomain`, and the patient app
 * has no notion of a disabled account — the admin area is the only thing that
 * reads or writes this. Adding it there would widen the app's model with a
 * concept no patient screen draws, and the server parses what it shares from
 * untrusted input, so a value the app cannot mean would become one it accepts.
 *
 * The wire values are `active` and `inactive`, which is what the web's
 * `AccountStatus` already uses. Accounts are never deleted, so the pair is
 * closed: an account keeps its history and is disabled instead, and the same
 * column is what re-enables it.
 */
enum class UserStatus(val wireValue: String) {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ;

    companion object {
        /**
         * Null for an unrecognised value, so a caller chooses between a 400 (a
         * client sent it) and a refusal (the server wrote it into the database).
         * The two want different answers, which is why this does not default.
         */
        fun fromWire(raw: String?): UserStatus? = entries.firstOrNull { it.wireValue == raw }
    }
}
