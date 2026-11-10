package com.example.mediq.ui.screens.doctordetails

/** Every slot the clinic offers on any day, in clock order. */
internal val AllTimeSlots = listOf(
    "9:00 AM",
    "9:30 AM",
    "10:00 AM",
    "10:30 AM",
    "11:00 AM",
    "11:30 AM",
    "12:00 PM",
    "12:30 PM",
    "1:00 PM",
    "1:30 PM",
    "2:00 PM",
    "2:30 PM",
    "3:00 PM",
    "3:30 PM",
    "4:00 PM",
    "4:30 PM"
)

/**
 * Expands the clinic's [available] and [blocked] sets onto the full day.
 *
 * Anything in neither set is [TimeSlotStatus.RESERVED]: a slot nobody opened
 * is treated as taken, not as free. A time in both sets counts as available,
 * because opening a slot is the later of the two decisions.
 */
internal fun buildSlotStatuses(
    available: Set<String>,
    blocked: Set<String>
): Map<String, TimeSlotStatus> = AllTimeSlots.associateWith { time ->
    when {
        time in available -> TimeSlotStatus.AVAILABLE
        time in blocked -> TimeSlotStatus.BLOCKED
        else -> TimeSlotStatus.RESERVED
    }
}
