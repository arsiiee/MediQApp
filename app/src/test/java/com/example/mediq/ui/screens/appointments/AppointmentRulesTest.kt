package com.example.mediq.ui.screens.appointments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * The appointment rules are pure data, so they are tested here rather than
 * through a device: cancelling, the tab split, and the status gate.
 */
class AppointmentRulesTest {

    private fun booking(
        id: String,
        status: AppointmentStatus = AppointmentStatus.Confirmed
    ) = AppointmentBooking(
        id = id,
        doctor = "Dr. Test",
        initials = "TT",
        specialty = "Testing",
        dateShort = "Mon, Sep 14",
        dateLong = "Monday, September 14, 2026",
        time = "9:30 AM",
        location = "Clinic 1",
        reason = "Routine",
        status = status,
        conversationId = "test_secretary"
    )

    @Test
    fun `only bookings that have not happened can be cancelled`() {
        assertTrue(AppointmentStatus.Confirmed.canCancel)
        assertTrue(AppointmentStatus.AwaitingConfirmation.canCancel)
        assertFalse(AppointmentStatus.Completed.canCancel)
        assertFalse(AppointmentStatus.Cancelled.canCancel)
    }

    @Test
    fun `cancelling moves the booking into history and marks it cancelled`() {
        val content = AppointmentsUiState.Content(
            upcoming = listOf(booking("1"), booking("2")),
            history = listOf(booking("3", AppointmentStatus.Completed))
        )

        val result = content.withCancelled(setOf("1"))

        assertEquals(listOf("2"), result.upcoming.map { it.id })
        assertEquals(listOf("3", "1"), result.history.map { it.id })
        assertEquals(
            AppointmentStatus.Cancelled,
            result.history.first { it.id == "1" }.status
        )
    }

    @Test
    fun `an unknown id changes nothing`() {
        val content = AppointmentsUiState.Content(
            upcoming = listOf(booking("1")),
            history = emptyList()
        )

        assertSame(content, content.withCancelled(setOf("99")))
    }

    @Test
    fun `an empty set changes nothing`() {
        val content = AppointmentsUiState.Content(
            upcoming = listOf(booking("1")),
            history = emptyList()
        )

        assertSame(content, content.withCancelled(emptySet()))
    }

    @Test
    fun `forTab reads the right list`() {
        val content = AppointmentsUiState.Content(
            upcoming = listOf(booking("1")),
            history = listOf(booking("2", AppointmentStatus.Completed))
        )

        assertEquals(listOf("1"), content.forTab(AppointmentsTab.Upcoming).map { it.id })
        assertEquals(listOf("2"), content.forTab(AppointmentsTab.History).map { it.id })
    }

    @Test
    fun `fromIndex maps the saved tab and rejects anything else`() {
        assertEquals(AppointmentsTab.Upcoming, AppointmentsTab.fromIndex(0))
        assertEquals(AppointmentsTab.History, AppointmentsTab.fromIndex(1))
        assertNull(AppointmentsTab.fromIndex(2))
        assertNull(AppointmentsTab.fromIndex(null))
    }

    @Test
    fun `the sample splits upcoming from history by cancellability`() {
        val content = AppointmentsSamples.content

        assertTrue(content.upcoming.all { it.status.canCancel })
        assertTrue(content.history.none { it.status.canCancel })
        assertNotNull(AppointmentsSamples.booking("1"))
        assertNull(AppointmentsSamples.booking("nope"))
    }
}
