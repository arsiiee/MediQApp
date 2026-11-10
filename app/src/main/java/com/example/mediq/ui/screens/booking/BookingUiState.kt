package com.example.mediq.ui.screens.booking

/**
 * What the patient is confirming.
 *
 * Built once by [BookingSamples] from the doctor and the day chosen on the
 * doctor-details screen, so the form and the result say the same thing.
 */
data class BookingSummary(
    val doctorName: String,
    val specialty: String,
    val location: String,
    val dateLabel: String,
    val time: String
)

/**
 * Where the confirmation has got to.
 *
 * "Slot taken" is separate from [Error] because the patient needs a different
 * way out: the slot is gone, so the only honest action is to pick another.
 */
sealed interface BookingSubmission {

    /** The form is open and nothing has been sent. */
    data object Idle : BookingSubmission

    /** The confirmation is on its way. */
    data object Submitting : BookingSubmission

    /** Another patient took the slot before this booking landed. */
    data class SlotTaken(val message: String) : BookingSubmission

    /** Anything else that stopped the booking; says what happened. */
    data class Error(val message: String) : BookingSubmission
}

/** What the success screen was handed. */
sealed interface BookingSuccessState {

    /** The slot is reserved. */
    data class Success(val summary: BookingSummary) : BookingSuccessState

    /** The slot went away between confirming and arriving here. */
    data class Conflict(val message: String) : BookingSuccessState
}

/** Callbacks both booking screens raise. */
data class BookingActions(
    val onBack: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onChooseAnotherSlot: () -> Unit = {},
    val onViewAppointments: () -> Unit = {},
    val onAddToCalendar: () -> Unit = {}
)
