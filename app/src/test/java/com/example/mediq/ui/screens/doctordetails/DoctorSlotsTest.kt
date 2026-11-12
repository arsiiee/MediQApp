package com.example.mediq.ui.screens.doctordetails

import com.example.mediq.data.model.ClinicHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behaviour of dynamic slot generation and status mapping.
 *
 * Plain JVM tests: functions are pure, so no emulator is involved.
 */
class DoctorSlotsTest {

    @Test
    fun `slots are generated from clinic hours windows`() {
        val slots = generateSlotLabelsForDay(
            listOf(
                ClinicHours(dayOfWeek = 1, startTime = "09:00:00", endTime = "10:30:00"),
                ClinicHours(dayOfWeek = 1, startTime = "13:00:00", endTime = "14:00:00")
            )
        )

        assertEquals(
            listOf("9:00 AM", "9:30 AM", "10:00 AM", "1:00 PM", "1:30 PM"),
            slots
        )
    }

    @Test
    fun `continuous windows include midday slots like 12pm`() {
        val slots = generateSlotLabelsForDay(
            listOf(ClinicHours(dayOfWeek = 1, startTime = "09:00:00", endTime = "17:00:00"))
        )

        assertTrue("12:00 PM" in slots)
        assertTrue("12:30 PM" in slots)
        assertTrue("1:00 PM" in slots)
    }

    @Test
    fun `booked and past slots are marked unavailable while open slots stay available`() {
        val options = buildSlotOptionsForDay(
            clinicHoursForDay = listOf(
                ClinicHours(dayOfWeek = 1, startTime = "09:00:00", endTime = "11:00:00")
            ),
            isToday = true,
            nowMinutes = 9 * 60, // 9:00 AM is past/current boundary
            userBookedTimes = setOf("9:30 AM"),
            otherBookedTimes = setOf("10:00 AM")
        )

        val statuses = options.associate { it.time to it.status }
        assertEquals(TimeSlotStatus.BLOCKED, statuses["9:00 AM"])
        assertEquals(TimeSlotStatus.USER_BOOKED, statuses["9:30 AM"])
        assertEquals(TimeSlotStatus.BLOCKED, statuses["10:00 AM"])
        assertEquals(TimeSlotStatus.AVAILABLE, statuses["10:30 AM"])
    }

    @Test
    fun `open slots are counted from generated schedule`() {
        val day = DateSchedule(
            option = DateOption(day = "Mon", date = 21, fullDate = "2026-09-21"),
            slots = listOf(
                TimeSlotOption("9:00 AM", TimeSlotStatus.AVAILABLE),
                TimeSlotOption("9:30 AM", TimeSlotStatus.AVAILABLE),
                TimeSlotOption("10:00 AM", TimeSlotStatus.BLOCKED)
            )
        )

        assertEquals(2, day.openSlots)
    }

    @Test
    fun `a schedule with nothing open reports no open slots`() {
        val full = DoctorDetailsUiState.Content(
            doctor = DoctorDetailsSamples.profile,
            schedule = DoctorDetailsSamples.scheduleWithNothingOpen(),
            userBookings = emptyList()
        )

        assertFalse(full.hasOpenSlots)
        assertTrue(DoctorDetailsSamples.content.hasOpenSlots)
    }
}
