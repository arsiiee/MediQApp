package com.example.mediq.ui.screens.appointments

/*
 * The appointment rules that are pure data, kept out of the composables so
 * they can be tested on the JVM.
 */

/**
 * Moves the bookings in [ids] out of upcoming and into history, marked
 * cancelled.
 *
 * A cancelled visit is history, not an upcoming one, so the patient finds it
 * where they left it rather than wondering where it went. Unknown ids are
 * ignored.
 */
fun AppointmentsUiState.Content.withCancelled(
    ids: Set<String>
): AppointmentsUiState.Content {
    if (ids.isEmpty() || upcoming.none { it.id in ids }) return this

    val cancelled = upcoming
        .filter { it.id in ids }
        .map { it.copy(status = AppointmentStatus.Cancelled) }

    return copy(
        upcoming = upcoming.filterNot { it.id in ids },
        history = history + cancelled
    )
}
