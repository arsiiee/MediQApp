package com.example.mediq.ui.screens.doctordetails

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour of [buildSlotStatuses] and the counts read from it.
 *
 * Plain JVM tests: both are pure functions, so no emulator is involved.
 */
class DoctorSlotsTest {

    @Test
    fun `every slot in the day gets a status`() {
        val statuses = buildSlotStatuses(
            available = setOf("9:00 AM"),
            blocked = setOf("10:00 AM")
        )

        assertEquals(AllTimeSlots.size, statuses.size)
    }

    @Test
    fun `a slot the clinic opened is available`() {
        val statuses = buildSlotStatuses(
            available = setOf("9:00 AM"),
            blocked = emptySet()
        )

        assertEquals(TimeSlotStatus.AVAILABLE, statuses["9:00 AM"])
    }

    @Test
    fun `a slot nobody opened is reserved, not free`() {
        val statuses = buildSlotStatuses(
            available = setOf("9:00 AM"),
            blocked = setOf("10:00 AM")
        )

        assertEquals(TimeSlotStatus.RESERVED, statuses["9:30 AM"])
        assertEquals(TimeSlotStatus.BLOCKED, statuses["10:00 AM"])
    }

    @Test
    fun `a slot in both sets counts as available`() {
        val statuses = buildSlotStatuses(
            available = setOf("9:00 AM"),
            blocked = setOf("9:00 AM")
        )

        assertEquals(TimeSlotStatus.AVAILABLE, statuses["9:00 AM"])
    }

    @Test
    fun `open slots are counted from the statuses, not written by hand`() {
        val day = DateSchedule(
            option = DateOption(day = "Mon", date = 21),
            statuses = buildSlotStatuses(
                available = setOf("9:00 AM", "9:30 AM"),
                blocked = setOf("10:00 AM")
            )
        )

        assertEquals(2, day.openSlots)
    }

    @Test
    fun `a schedule with nothing open reports no open slots`() {
        val full = DoctorDetailsUiState.Content(
            doctor = DoctorDetailsSamples.profile,
            schedule = DoctorDetailsSamples.scheduleWithNothingOpen()
        )

        assertFalse(full.hasOpenSlots)
        assertTrue(DoctorDetailsSamples.content.hasOpenSlots)
    }
}
