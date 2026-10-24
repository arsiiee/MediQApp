package com.example.mediq.ui.screens.home

/*
 * The current home data source: fixed sample content.
 *
 * This file is the seam between layout and data. When a repository replaces it,
 * only HomeScreen's default argument changes.
 */

object HomeSamples {

    private val patient = PatientSummary(
        firstName = "Arweyne",
        fullName = "Arweyne Zoe Salcedo"
    )

    private val specialties = listOf(
        "Internal Medicine",
        "Pediatrics",
        "Cardiology",
        "Dermatology",
        "OB-Gynecology",
        "Orthopedics",
        "Ophthalmology"
    )

    private val openSlots = listOf(
        DoctorAvailability(
            doctorId = "joel-marquez",
            name = "Dr. Joel Marquez",
            specialty = "Pediatrics",
            location = "Annex Wing — 1F",
            openSlots = 18
        ),
        DoctorAvailability(
            doctorId = "grace-villanueva",
            name = "Dr. Grace Villanueva",
            specialty = "OB-Gynecology",
            location = "Annex Wing — 4F",
            openSlots = 15
        ),
        DoctorAvailability(
            doctorId = "ramon-dela-cruz",
            name = "Dr. Ramon Dela Cruz",
            specialty = "Orthopedics",
            location = "Main Building — 2F",
            openSlots = 15
        )
    )

    private val nextAppointment = NextAppointment(
        id = "1",
        time = "9:30 AM",
        date = "Monday, September 14, 2026",
        doctorName = "Dr. Maria Elena Sandoval",
        specialty = "Internal Medicine",
        location = "Main Building — 2F — Clinic 204",
        arrivalNote = "Arrive 15 minutes early for registration",
        status = AppointmentStatus.Confirmed
    )

    /** Loaded, with an upcoming appointment. */
    val content = HomeUiState.Content(
        patient = patient,
        nextAppointment = nextAppointment,
        specialties = specialties,
        openSlots = openSlots,
        unreadNotifications = 1
    )

    /** Loaded, nothing booked. */
    val nothingBooked = content.copy(nextAppointment = null, unreadNotifications = 0)

    /** Loaded, no availability to show. */
    val noOpenSlots = content.copy(openSlots = emptyList())

    val loading = HomeUiState.Loading

    val error = HomeUiState.Error(
        message = "Check your connection and try again."
    )
}
