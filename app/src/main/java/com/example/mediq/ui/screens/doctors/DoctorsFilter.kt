package com.example.mediq.ui.screens.doctors

/*
 * Doctor filtering.
 *
 * Pure functions and fixed option lists, kept out of the composables so the
 * behaviour is unit-testable without a device.
 */

const val AllSpecialties = "All specialties"
const val AllLocations = "All locations"

val specialtyOptions = listOf(
    AllSpecialties,
    "Internal Medicine",
    "Pediatrics",
    "Cardiology",
    "OB-Gynecology",
    "Orthopedics",
    "Dermatology",
    "Ophthalmology"
)

val locationOptions = listOf(
    AllLocations,
    "Main Building — 2F",
    "Main Building — 3F",
    "Annex Wing — 1F",
    "Annex Wing — 4F"
)

/**
 * The doctors matching [filters].
 *
 * - The query matches the doctor's name or specialty, case-insensitively.
 * - A specialty filter is an exact match; "All specialties" matches everything.
 * - A location filter matches the start of the doctor's room string, so
 *   "Annex Wing — 1F" also matches "Annex Wing — 1F · Clinic 108".
 */
fun filterDoctors(
    doctors: List<DoctorInfo>,
    filters: DoctorFilters
): List<DoctorInfo> = doctors.filter { doctor ->

    val matchesQuery = filters.query.isBlank() ||
            doctor.name.contains(filters.query, ignoreCase = true) ||
            doctor.specialty.contains(filters.query, ignoreCase = true)

    val matchesSpecialty = filters.specialty == AllSpecialties ||
            doctor.specialty == filters.specialty

    val matchesLocation = filters.location == AllLocations ||
            doctor.location.startsWith(filters.location)

    matchesQuery && matchesSpecialty && matchesLocation
}
