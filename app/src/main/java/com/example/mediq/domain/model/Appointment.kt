package com.example.mediq.domain.model

import java.time.Instant

/**
 * Where an appointment is in its lifecycle.
 *
 * This is an enum rather than a string because the screens branch on it — the
 * upcoming/history split, whether Cancel is offered, which colour the status
 * badge uses. With strings, a server sending "confirmed" instead of
 * "Confirmed" silently breaks all of that.
 */
enum class AppointmentStatus(val wireValue: String, val displayName: String) {
    /** Booked, and the clinic secretary has not responded yet. */
    PENDING_CONFIRMATION("pending_confirmation", "Awaiting confirmation"),

    CONFIRMED("confirmed", "Confirmed"),

    COMPLETED("completed", "Completed"),

    CANCELLED("cancelled", "Cancelled"),

    /** The clinic declined, e.g. the doctor fell ill. */
    DECLINED("declined", "Declined"),

    /**
     * The server sent a value this build does not recognise.
     *
     * Not a normal state — the server never emits it. It exists so that a renamed
     * or added wire value is *shown* as unknown rather than silently resolved to
     * [PENDING_CONFIRMATION], which rendered a cancelled appointment as awaiting
     * confirmation. A patient deciding whether to travel needs the truth, and
     * "Unknown" is the truth.
     *
     * Not actionable and not upcoming: if the real status cannot be read, the
     * app must not offer Cancel on the strength of a guess, and must not put it
     * on the Upcoming tab as though it were settled.
     */
    UNKNOWN("unknown", "Unknown"),
    ;

    /** Appointments that belong on the "Upcoming" tab. */
    val isUpcoming: Boolean
        get() = this == PENDING_CONFIRMATION || this == CONFIRMED

    /** Whether the patient can still cancel or ask to move it. */
    val isActionable: Boolean
        get() = this == PENDING_CONFIRMATION || this == CONFIRMED
}

data class Appointment(
    val id: String,
    val doctor: Doctor,
    val startsAt: Instant,
    val endsAt: Instant,
    val status: AppointmentStatus,
    val location: ClinicLocation,
    val fee: Money,
    /** Why the patient wants to be seen. Free text from the booking form. */
    val reasonForVisit: String?,
) {
    fun isPast(now: Instant): Boolean = startsAt.isBefore(now)

    /**
     * Whether this appointment belongs on the "Upcoming" tab.
     *
     * [AppointmentStatus.UNKNOWN] counts as history on purpose. It is neither
     * confirmed nor upcoming-and-certain, and dropping it from both tabs would
     * hide an appointment from the patient entirely — the worst outcome, since
     * the reason it is unknown is that something changed.
     */
    fun belongsInHistory(now: Instant): Boolean =
        status == AppointmentStatus.COMPLETED ||
            status == AppointmentStatus.CANCELLED ||
            status == AppointmentStatus.DECLINED ||
            status == AppointmentStatus.UNKNOWN ||
            isPast(now)
}

/** What the patient asked to change when requesting a reschedule. */
data class RescheduleRequest(
    val appointmentId: String,
    val requestedSlotId: String,
    val note: String? = null,
)