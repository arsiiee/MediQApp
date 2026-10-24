package com.example.mediq.ui.screens.doctors

/*
 * The current doctors data source: fixed sample content.
 *
 * Replacing this with a repository changes only DoctorsScreen's default argument.
 */

object DoctorsSamples {

    private val doctors = listOf(
        DoctorInfo(
            id = "1",
            name = "Dr. Maria Elena Sandoval",
            initials = "ME",
            specialty = "Internal Medicine",
            experience = 14,
            fee = 700,
            location = "Main Building — 2F · Clinic 204",
            openSlots = 13
        ),
        DoctorInfo(
            id = "2",
            name = "Dr. Joel Marquez",
            initials = "JM",
            specialty = "Pediatrics",
            experience = 18,
            fee = 500,
            location = "Annex Wing — 1F · Clinic 108",
            openSlots = 18
        ),
        DoctorInfo(
            id = "3",
            name = "Dr. Antonio Reyes Jr.",
            initials = "AR",
            specialty = "Cardiology",
            experience = 20,
            fee = 1000,
            location = "Main Building — 3F · Heart Station 301",
            openSlots = 10
        ),
        DoctorInfo(
            id = "4",
            name = "Dr. Grace Villanueva",
            initials = "GV",
            specialty = "OB-Gynecology",
            experience = 15,
            fee = 800,
            location = "Annex Wing — 4F · Clinic 402",
            openSlots = 15
        ),
        DoctorInfo(
            id = "5",
            name = "Dr. Ramon Dela Cruz",
            initials = "RD",
            specialty = "Orthopedics",
            experience = 13,
            fee = 900,
            location = "Main Building — 2F · Clinic 212",
            openSlots = 15
        )
    )

    /** Loaded, with the full catalogue. */
    val loaded = DoctorsUiState.Content(doctors)

    /** Loaded, but the clinic has published no doctors. */
    val empty = DoctorsUiState.Content(emptyList())

    val loading = DoctorsUiState.Loading

    val error = DoctorsUiState.Error(
        message = "Check your connection and try again."
    )
}
