package com.example.mediq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Covers the `Next open: Tue 09:00` line on the doctors list.
 *
 * That line is derived from a doctor's weekly [ClinicHours] rather than read per
 * row, because a per-row availability call is N+1 requests on a list for a value
 * the list already holds. The slot count the figure also draws is deliberately
 * not derived here - only `getAvailability` knows it - so it is not tested.
 *
 * **2026-10-15 is a Thursday**, so `now` below is Thursday 10:00 at the clinic.
 */
class NextClinicSessionTest {

    private val thursdayMorning = clinicDateTime(
        LocalDate.of(2026, 10, 15),
        LocalTime.of(10, 0),
    )

    private fun hours(day: DayOfWeek, opensAt: String) = ClinicHours(
        dayOfWeek = day,
        opensAt = LocalTime.parse(opensAt),
        closesAt = LocalTime.parse(opensAt).plusHours(3),
    )

    @Test
    fun `a doctor with no hours has no next open`() {
        assertNull(nextClinicSession(emptyList(), thursdayMorning))
    }

    @Test
    fun `a session later today beats one earlier in the week`() {
        val sessions = listOf(
            hours(DayOfWeek.MONDAY, "09:00"),
            hours(DayOfWeek.THURSDAY, "14:00"),
        )

        assertEquals(
            hours(DayOfWeek.THURSDAY, "14:00"),
            nextClinicSession(sessions, thursdayMorning),
        )
    }

    @Test
    fun `a session that already opened today is next week's, not today's`() {
        val sessions = listOf(
            hours(DayOfWeek.THURSDAY, "09:00"),
            hours(DayOfWeek.MONDAY, "09:00"),
        )

        // 09:00 has passed at 10:00, so Monday is nearer than next Thursday: 4
        // days away against 7. Without the wrap this would answer Thursday 09:00,
        // a time already gone.
        assertEquals(
            hours(DayOfWeek.MONDAY, "09:00"),
            nextClinicSession(sessions, thursdayMorning),
        )
    }

    @Test
    fun `the soonest upcoming day wins, not the first in the list`() {
        val sessions = listOf(
            hours(DayOfWeek.TUESDAY, "09:00"),
            hours(DayOfWeek.FRIDAY, "09:00"),
        )

        assertEquals(
            hours(DayOfWeek.FRIDAY, "09:00"),
            nextClinicSession(sessions, thursdayMorning),
        )
    }

    @Test
    fun `a session opening exactly now counts as today's`() {
        val sessions = listOf(
            hours(DayOfWeek.THURSDAY, "10:00"),
            hours(DayOfWeek.FRIDAY, "09:00"),
        )

        assertEquals(
            hours(DayOfWeek.THURSDAY, "10:00"),
            nextClinicSession(sessions, thursdayMorning),
        )
    }

    @Test
    fun `the answer uses the clinic's zone, not the phone's`() {
        // 2026-10-15T17:00Z is Friday 01:00 in Asia/Manila but still Thursday
        // 17:00 in UTC, so the two zones pick different sessions: from the
        // clinic's Friday it is Friday's 09:00, from UTC's Thursday it is
        // Thursday's 20:00. An instant that only differs in the time of day -
        // 02:00Z, say - shares the date in both zones and would pass either way,
        // which is why this one crosses midnight.
        val instant = Instant.parse("2026-10-15T17:00:00Z")
        val sessions = listOf(
            hours(DayOfWeek.THURSDAY, "20:00"),
            hours(DayOfWeek.FRIDAY, "09:00"),
        )

        assertEquals(
            hours(DayOfWeek.FRIDAY, "09:00"),
            nextClinicSession(sessions, instant),
        )
    }
}
