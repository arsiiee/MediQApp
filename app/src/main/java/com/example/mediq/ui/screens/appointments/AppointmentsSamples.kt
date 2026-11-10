package com.example.mediq.ui.screens.appointments

import com.example.mediq.ui.session.DateTimeFormatter

/*
 * The current appointments data source: fixed sample content.
 *
 * Replacing this with a repository changes only AppointmentsScreen's default
 * argument. The detail screen reads the same bookings, so the two cannot drift.
 *
 * Dates and times come from [DateTimeFormatter] so the list, the detail screen
 * and the home card never disagree on how a visit is shown.
 */

object AppointmentsSamples {

    private val bookings = listOf(
        AppointmentBooking(
            id = "1",
            doctor = "Dr. Maria Elena Sandoval",
            initials = "ME",
            specialty = "Internal Medicine",
            dateShort = DateTimeFormatter.formatDate("2026-09-14T09:30:00"),
            dateLong = DateTimeFormatter.formatDateLong("2026-09-14T09:30:00"),
            time = DateTimeFormatter.formatTime("2026-09-14T09:30:00"),
            location = "Main Building — 2F · Clinic 204",
            reason = "Follow-up for elevated blood pressure",
            status = AppointmentStatus.Confirmed,
            conversationId = "maria_secretary"
        ),
        AppointmentBooking(
            id = "2",
            doctor = "Dr. Kathleen Lim",
            initials = "KL",
            specialty = "Dermatology",
            dateShort = DateTimeFormatter.formatDate("2026-09-17T10:00:00"),
            dateLong = DateTimeFormatter.formatDateLong("2026-09-17T10:00:00"),
            time = DateTimeFormatter.formatTime("2026-09-17T10:00:00"),
            location = "Annex Wing — 1F · Clinic 114",
            reason = "Recurring skin rash on both arms",
            status = AppointmentStatus.AwaitingConfirmation,
            conversationId = "kathleen_secretary"
        ),
        AppointmentBooking(
            id = "3",
            doctor = "Dr. Ramon Dela Cruz",
            initials = "RD",
            specialty = "Orthopedics",
            dateShort = DateTimeFormatter.formatDate("2026-08-24T14:00:00"),
            dateLong = DateTimeFormatter.formatDateLong("2026-08-24T14:00:00"),
            time = DateTimeFormatter.formatTime("2026-08-24T14:00:00"),
            location = "Main Building — 2F · Clinic 212",
            reason = "Knee pain consultation",
            status = AppointmentStatus.Completed,
            conversationId = "joel_secretary"
        ),
        AppointmentBooking(
            id = "4",
            doctor = "Dr. Grace Villanueva",
            initials = "GV",
            specialty = "OB-Gynecology",
            dateShort = DateTimeFormatter.formatDate("2026-08-12T11:30:00"),
            dateLong = DateTimeFormatter.formatDateLong("2026-08-12T11:30:00"),
            time = DateTimeFormatter.formatTime("2026-08-12T11:30:00"),
            location = "Annex Wing — 4F · Clinic 402",
            reason = "Routine consultation",
            status = AppointmentStatus.Completed,
            // Grace has no separate thread yet; the clinic desk answers.
            conversationId = "maria_secretary"
        )
    )

    /** Loaded, with both tabs filled. */
    val content: AppointmentsUiState.Content = AppointmentsUiState.Content(
        upcoming = bookings.filter { it.status.canCancel },
        history = bookings.filterNot { it.status.canCancel }
    )

    /** Loaded, but the patient has never booked anything. */
    val empty: AppointmentsUiState.Content = AppointmentsUiState.Content(
        upcoming = emptyList(),
        history = emptyList()
    )

    /** Loaded, with nothing left to come. */
    val pastOnly: AppointmentsUiState.Content = AppointmentsUiState.Content(
        upcoming = emptyList(),
        history = bookings.filterNot { it.status.canCancel }
    )

    val loading: AppointmentsUiState = AppointmentsUiState.Loading

    val error: AppointmentsUiState = AppointmentsUiState.Error(
        message = "Check your connection and try again."
    )

    /** The booking behind [id], or null when nothing matches. */
    fun booking(id: String?): AppointmentBooking? = bookings.firstOrNull { it.id == id }
}
