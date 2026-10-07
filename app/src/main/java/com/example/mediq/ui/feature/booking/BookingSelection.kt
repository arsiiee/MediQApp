package com.example.mediq.ui.feature.booking

/**
 * The slot the patient picked on the doctor's profile.
 *
 * Held here rather than passed through navigation arguments because it is a
 * real object, not a string — the booking screen needs the doctor, the fee,
 * and the location, and encoding all of that into a route string would mean
 * the app could build a request for a doctor and a time that don't match a
 * real slot.
 *
 * Cleared once the booking succeeds so a stale pick can't be re-submitted.
 */
object BookingSelection {

    data class Selection(
        val doctorId: String,
        val slotId: String,
        val doctorDisplayName: String,
        val specialtyDisplayName: String,
        val startsAt: java.time.Instant,
        val locationDisplay: String,
        val feeCentavos: Long,
    )

    private var current: Selection? = null

    val selection: Selection? get() = current

    fun set(selection: Selection) {
        current = selection
    }

    fun clear() {
        current = null
    }
}