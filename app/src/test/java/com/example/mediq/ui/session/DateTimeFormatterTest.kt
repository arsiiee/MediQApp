package com.example.mediq.ui.session

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import com.example.mediq.data.model.ClinicLocation

class DateTimeFormatterTest {

    @Test
    fun `formats a short date as Mon, Oct 19`() {
        assertEquals("Mon, Oct 19", DateTimeFormatter.formatDate("2026-10-19T14:00:00", Locale.US))
    }

    @Test
    fun `formats a short date regardless of the time part`() {
        assertEquals("Tue, Oct 27", DateTimeFormatter.formatDate("2026-10-27T00:00:00", Locale.US))
    }

    @Test
    fun `formats a time in 12-hour clock`() {
        assertEquals("2:00 PM", DateTimeFormatter.formatTime("2026-10-19T14:00:00", Locale.US))
    }

    @Test
    fun `formats a single-digit hour without a leading zero`() {
        assertEquals("9:05 AM", DateTimeFormatter.formatTime("2026-10-19T09:05:00", Locale.US))
    }

    @Test
    fun `maps midnight and noon to 12-hour forms`() {
        assertEquals("12:00 AM", DateTimeFormatter.formatTime("2026-10-19T00:00:00", Locale.US))
        assertEquals("12:00 PM", DateTimeFormatter.formatTime("2026-10-19T12:00:00", Locale.US))
    }

    @Test
    fun `formats a long date`() {
        assertEquals("Monday, September 14, 2026",
            DateTimeFormatter.formatDateLong("2026-09-14T09:30:00", Locale.US))
    }

    @Test
    fun `formats a complete location`() {
        assertEquals("MediQ Medical Center · Floor 2 · Room 205",
            DateTimeFormatter.formatLocation(ClinicLocation("MediQ Medical Center", "2", "205")))
    }

    @Test
    fun `skips a missing floor`() {
        assertEquals("MediQ Medical Center · Room 205",
            DateTimeFormatter.formatLocation(ClinicLocation("MediQ Medical Center", "", "205")))
    }

    @Test
    fun `skips a missing room`() {
        assertEquals("MediQ Medical Center · Floor 2",
            DateTimeFormatter.formatLocation(ClinicLocation("MediQ Medical Center", "2", "")))
    }

    @Test
    fun `skips floor and room when both are missing`() {
        assertEquals("MediQ Medical Center",
            DateTimeFormatter.formatLocation(ClinicLocation("MediQ Medical Center", "", "")))
    }

    @Test
    fun `returns an empty string for a null location`() {
        assertEquals("", DateTimeFormatter.formatLocation(null))
    }

    @Test
    fun `omits a blank building too`() {
        assertEquals("Floor 2 · Room 205",
            DateTimeFormatter.formatLocation(ClinicLocation("", "2", "205")))
    }

    // ---- Message timestamp formatting ----

    @Test
    fun `formats a message timestamp from today as hour minute am pm`() {
        val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
        val iso = now.toString()
        val result = DateTimeFormatter.formatMessageTime(iso, java.util.Locale.US)
        assertEquals(
            now.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US)),
            result
        )
    }

    @Test
    fun `formats a message timestamp from yesterday as Yesterday`() {
        val yesterday = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault()).minusDays(1)
        val iso = yesterday.toString()
        assertEquals("Yesterday", DateTimeFormatter.formatMessageTime(iso, java.util.Locale.US))
    }

    @Test
    fun `formats a message timestamp from older than yesterday as MMM d`() {
        val older = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault()).minusDays(2)
        val iso = older.toString()
        val expected = older.format(java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.US))
        assertEquals(expected, DateTimeFormatter.formatMessageTime(iso, java.util.Locale.US))
    }

    @Test
    fun `returns empty string for an unparseable timestamp`() {
        assertEquals("", DateTimeFormatter.formatMessageTime("not-a-timestamp", java.util.Locale.US))
        assertEquals("", DateTimeFormatter.formatMessageTime("", java.util.Locale.US))
    }
}
