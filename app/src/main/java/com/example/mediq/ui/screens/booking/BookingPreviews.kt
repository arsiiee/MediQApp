package com.example.mediq.ui.screens.booking

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

/*
 * Every booking state, renderable without a device.
 *
 * The form previews reuse one summary so the four submission states read as
 * the same screen moving forward, not four different screens.
 */

private val sampleSummary: BookingSummary
    get() = BookingSamples.summary(dateIndex = 0, time = "10:00 AM")

@Preview(name = "Booking — form", showBackground = true, heightDp = 1100)
@Composable
private fun BookingFormPreview() {
    MediQTheme {
        BookingFlowContent(
            summary = sampleSummary,
            submission = BookingSamples.idle,
            actions = BookingActions()
        )
    }
}

@Preview(name = "Booking — submitting", showBackground = true, heightDp = 1100)
@Composable
private fun BookingSubmittingPreview() {
    MediQTheme {
        BookingFlowContent(
            summary = sampleSummary,
            submission = BookingSamples.submitting,
            actions = BookingActions()
        )
    }
}

@Preview(name = "Booking — slot taken", showBackground = true, heightDp = 1100)
@Composable
private fun BookingSlotTakenPreview() {
    MediQTheme {
        BookingFlowContent(
            summary = sampleSummary,
            submission = BookingSamples.slotTaken,
            actions = BookingActions()
        )
    }
}

@Preview(name = "Booking — error", showBackground = true, heightDp = 1100)
@Composable
private fun BookingErrorPreview() {
    MediQTheme {
        BookingFlowContent(
            summary = sampleSummary,
            submission = BookingSamples.error,
            actions = BookingActions()
        )
    }
}

@Preview(name = "Booking — success", showBackground = true, heightDp = 900)
@Composable
private fun BookingSuccessPreview() {
    MediQTheme {
        BookingSuccessContent(
            state = BookingSamples.success(dateIndex = 0, time = "10:00 AM"),
            actions = BookingActions()
        )
    }
}

@Preview(name = "Booking — conflict", showBackground = true, heightDp = 900)
@Composable
private fun BookingConflictPreview() {
    MediQTheme {
        BookingSuccessContent(
            state = BookingSamples.conflict,
            actions = BookingActions()
        )
    }
}
