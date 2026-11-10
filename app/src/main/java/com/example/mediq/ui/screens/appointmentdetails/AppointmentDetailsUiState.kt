package com.example.mediq.ui.screens.appointmentdetails

import com.example.mediq.ui.screens.appointments.AppointmentBooking

/** What the detail screen was handed. */
sealed interface AppointmentDetailsUiState {

    /** Skeleton that mirrors the loaded card. */
    data object Loading : AppointmentDetailsUiState

    /** The booking is loaded. */
    data class Content(val booking: AppointmentBooking) : AppointmentDetailsUiState

    /** Nothing to show, and why. Covers a failed load and an unknown id. */
    data class Error(val message: String) : AppointmentDetailsUiState
}

/** Callbacks the detail screen raises. */
data class AppointmentDetailsActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onChatWithSecretary: (conversationId: String) -> Unit = {},
    /** Confirms the cancellation; the caller reports it back to the list. */
    val onCancelAppointment: (appointmentId: String) -> Unit = {}
)
