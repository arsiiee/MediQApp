package com.example.mediq.ui.screens.booking

import com.example.mediq.ui.screens.doctordetails.DoctorDetailsSamples

/*
 * The current booking data source: fixed sample content.
 *
 * The doctor comes from [DoctorDetailsSamples] so the form, the success screen
 * and the details screen cannot drift apart on the name or the clinic.
 */

object BookingSamples {

    /*
     * Day labels by index. Must stay aligned with the strip in the
     * doctordetails package: that screen passes the index of the day it
     * shows, and this table is what resolves it to a date.
     */
    private val dateLabels = listOf(
        "Monday, September 21, 2026",
        "Wednesday, September 23, 2026",
        "Friday, September 25, 2026",
        "Monday, September 28, 2026",
        "Wednesday, September 30, 2026"
    )

    /** The chosen day, or the first one when the index is out of range. */
    fun dateLabel(dateIndex: Int): String =
        dateLabels.getOrElse(dateIndex) { dateLabels.first() }

    fun summary(
        dateIndex: Int,
        time: String
    ): BookingSummary {
        val doctor = DoctorDetailsSamples.profile

        return BookingSummary(
            doctorName = doctor.name,
            specialty = doctor.specialty,
            location = doctor.location,
            dateLabel = dateLabel(dateIndex),
            time = time
        )
    }

    val idle: BookingSubmission = BookingSubmission.Idle

    val submitting: BookingSubmission = BookingSubmission.Submitting

    val slotTaken: BookingSubmission = BookingSubmission.SlotTaken(
        message = "Another patient booked this slot while you were filling " +
                "the form. Pick a different time to continue."
    )

    val error: BookingSubmission = BookingSubmission.Error(
        message = "We couldn't confirm the booking. Check your connection " +
                "and try again."
    )

    fun success(dateIndex: Int, time: String) =
        BookingSuccessState.Success(summary(dateIndex, time))

    val conflict: BookingSuccessState = BookingSuccessState.Conflict(
        message = "This slot was taken while your booking was being " +
                "confirmed. Nothing has been reserved."
    )
}
