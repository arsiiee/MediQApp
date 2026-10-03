package com.example.mediq.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Who is signed in. Sign-in promises the role is detected server-side, so the
 * app never asks for it — it just reads what the login response returned.
 */
enum class UserRole(val wireValue: String) {
    PATIENT("patient"),
    DOCTOR("doctor"),

    /** Staff who confirm and manage the clinic's schedule. */
    SECRETARY("secretary"),
    ADMIN("admin"),
}

/**
 * Sex, as recorded for a patient's chart. [PREFER_NOT_TO_SAY] exists because
 * the profile screen collected this field, and a required single choice is not
 * something to force on someone.
 */
enum class Sex(val wireValue: String, val displayName: String) {
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    OTHER("other", "Other"),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "Prefer not to say"),
}

data class UserProfile(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    /** In E.164 form, e.g. "+639175550142". */
    val mobileNumber: String,
    val dateOfBirth: LocalDate,
    val sex: Sex?,
    val address: String?,
    val role: UserRole,
) {
    val initials: String
        get() = fullName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
}

/** What the app holds about the signed-in session. */
data class AuthSession(
    val accessToken: String,
    val expiresAt: Instant,
    val profile: UserProfile,
) {
    fun isExpired(now: Instant): Boolean = now.isAfter(expiresAt)
}

/** The fields collected across the registration screens, before they become an account. */
data class RegistrationDraft(
    val fullName: String,
    val mobileNumber: String,
    val otp: String,
    val username: String,
    val password: String,
)