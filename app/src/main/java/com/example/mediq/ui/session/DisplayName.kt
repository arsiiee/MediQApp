package com.example.mediq.ui.session

/*
 * Turning names into the pieces the screens show.
 *
 * Plain Kotlin, no Compose and no Android, so the rules can be pinned down by
 * a unit test rather than by reading the screens.
 */
object DisplayName {

    /**
     * The first given name of a full name, e.g. "Arweyne Zoe Salcedo" ->
     * "Arweyne".
     */
    fun firstName(fullName: String): String = fullName
        .trim()
        .split(' ', '-')
        .firstOrNull { it.isNotBlank() }
        .orEmpty()

    /**
     * Up to two initials, e.g. "Arweyne Zoe Salcedo" -> "AZ".
     *
     * Derived rather than hardcoded, so the avatar always follows the name it
     * is drawn next to.
     */
    fun initials(fullName: String): String = fullName
        .split(' ', '-')
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")

    /**
     * A readable name for a username, e.g. "kissie.apple" -> "Kissie Apple".
     *
     * Used when the account was not created on this device, so the username is
     * the only name available. Never better than the real name — it is a
     * fallback, not a guess at the patient's identity.
     */
    fun fromUsername(username: String): String = username
        .split('.', '_', '-', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { it.uppercaseChar() }
        }
}
