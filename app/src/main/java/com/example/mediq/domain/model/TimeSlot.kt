package com.example.mediq.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Whether a slot can be booked, and if not, why. */
enum class SlotStatus(val wireValue: String) {
    AVAILABLE("available"),

    /** Someone else already booked it. */
    RESERVED("reserved"),

    /** The clinic blocked it, e.g. a meeting or leave. */
    BLOCKED("blocked"),

    /**
     * The server sent a value this build does not recognise.
     *
     * Not bookable. That is the whole point: the previous fallback for an
     * unknown value was [AVAILABLE], which would offer a slot whose real state
     * is unknown and let a patient tap through to a booking that fails — or
     * worse, succeeds against a slot that should not have been free. When the
     * answer is not known, the safe answer is no.
     */
    UNKNOWN("unknown"),
}

/**
 * A bookable window in a doctor's schedule.
 *
 * [startsAt] is an absolute instant, so two slots are never compared as
 * "9:30 AM" strings. Availability is sent by the server because the clinic owns
 * it — a slot that was open a minute ago may be taken by the time the user
 * taps it.
 */
data class TimeSlot(
    val id: String,
    val doctorId: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val status: SlotStatus,
) {
    val isBookable: Boolean get() = status == SlotStatus.AVAILABLE

    val duration: Duration get() = Duration.between(startsAt, endsAt)
}

/**
 * A date the clinic has slots on, used to fill the date picker without
 * fetching every slot for every date up front.
 */
data class AvailableDate(
    val date: LocalDate,
    val openSlotCount: Int,
)