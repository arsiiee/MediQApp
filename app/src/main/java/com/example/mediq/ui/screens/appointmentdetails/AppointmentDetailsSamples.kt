package com.example.mediq.ui.screens.appointmentdetails

import com.example.mediq.ui.screens.appointments.AppointmentsSamples

/*
 * The current detail data source: the same bookings the list shows.
 *
 * Reading [AppointmentsSamples] instead of keeping a second copy is what stops
 * the day here from drifting from the day in the list.
 */

object AppointmentDetailsSamples {

    /** The booking behind [appointmentId], or a "not found" error. */
    fun forId(appointmentId: String?): AppointmentDetailsUiState {
        val booking = AppointmentsSamples.booking(appointmentId)
            ?: return AppointmentDetailsUiState.Error(
                message = "This appointment is no longer in your records."
            )

        return AppointmentDetailsUiState.Content(booking)
    }

    val loading: AppointmentDetailsUiState = AppointmentDetailsUiState.Loading

    val notFound: AppointmentDetailsUiState = AppointmentDetailsUiState.Error(
        message = "This appointment is no longer in your records."
    )
}
