package com.example.mediq.ui.screens.booking

import com.example.mediq.ui.screens.doctordetails.DoctorDetailsSamples
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The date table is the only booking logic worth a JVM test: it is what
 * turns the day-strip index into the sentence the patient reads.
 */
class BookingDateLabelTest {

    @Test
    fun `known index resolves to its label`() {
        assertEquals(
            "Monday, September 21, 2026",
            BookingSamples.dateLabel(0)
        )
    }

    @Test
    fun `last known index resolves to its label`() {
        assertEquals(
            "Wednesday, September 30, 2026",
            BookingSamples.dateLabel(4)
        )
    }

    @Test
    fun `out of range index falls back to the first day`() {
        assertEquals(
            "Monday, September 21, 2026",
            BookingSamples.dateLabel(99)
        )
    }

    @Test
    fun `negative index falls back to the first day`() {
        assertEquals(
            "Monday, September 21, 2026",
            BookingSamples.dateLabel(-1)
        )
    }

    @Test
    fun `summary carries the chosen day and time through`() {
        val summary = BookingSamples.summary(dateIndex = 1, time = "2:30 PM")

        assertEquals("Wednesday, September 23, 2026", summary.dateLabel)
        assertEquals("2:30 PM", summary.time)
        assertEquals(
            DoctorDetailsSamples.profile.name,
            summary.doctorName
        )
    }
}
