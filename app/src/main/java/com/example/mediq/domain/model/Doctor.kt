package com.example.mediq.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Where a doctor sees patients, broken into parts rather than kept as a single
 * display string. The screen decides how to join these into "Main Building —
 * 2F — Clinic 204"; the server never has to know that format.
 */
data class ClinicLocation(
    val building: String,
    val floor: String,
    val room: String,
)

/**
 * One recurring clinic session, e.g. Monday 09:00 to 12:00. Repeated weekly,
 * so this holds a weekday and times of day rather than a date.
 */
data class ClinicHours(
    val dayOfWeek: DayOfWeek,
    val opensAt: LocalTime,
    val closesAt: LocalTime,
)

/** The languages a doctor can consult in. */
enum class ConsultationLanguage(val wireValue: String, val displayName: String) {
    ENGLISH("en", "English"),
    FILIPINO("fil", "Filipino"),
    CEBUANO("ceb", "Cebuano"),
}

data class Doctor(
    val id: String,
    val fullName: String,
    val specialty: Specialty,
    /** Years since the doctor started practising. */
    val yearsOfExperience: Int,
    val consultationFee: Money,
    val location: ClinicLocation,
    /** Professional licence number, as shown on the profile. */
    val licenseNumber: String,
    /** Longer description of what the doctor treats. */
    val bio: String,
    val languages: List<ConsultationLanguage>,
    val clinicHours: List<ClinicHours>,
) {
    /**
     * Convenience for list rows, which show a doctor by name and specialty
     * without the full profile.
     */
    val displayName: String get() = "Dr. $fullName"

    val initials: String
        get() = fullName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
}