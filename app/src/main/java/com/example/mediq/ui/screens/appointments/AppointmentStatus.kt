package com.example.mediq.ui.screens.appointments

/**
 * Where a booking is in its life, and whether the patient can still pull out.
 *
 * The label is the text the chip shows, so status is never carried by colour
 * alone.
 */
enum class AppointmentStatus(val label: String) {

    /** The clinic has confirmed the slot. */
    Confirmed("Confirmed"),

    /** Waiting on the clinic to confirm. */
    AwaitingConfirmation("Awaiting confirmation"),

    /** The visit already happened. */
    Completed("Completed"),

    /** The patient pulled out. */
    Cancelled("Cancelled");

    /** Only a booking that has not happened yet can be cancelled. */
    val canCancel: Boolean
        get() = this == Confirmed || this == AwaitingConfirmation
}
