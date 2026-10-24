package com.example.mediq.ui.screens.appointmentdetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

/*
 * Every appointment-detail state, renderable without a device.
 *
 * One preview per status: the screen changes what it offers (cancel or not) as
 * the booking moves, so each status is worth seeing on its own.
 */

@Preview(name = "Appointment — confirmed", showBackground = true, heightDp = 1000)
@Composable
private fun AppointmentConfirmedPreview() {
    MediQTheme {
        AppointmentDetailsContent(
            state = AppointmentDetailsSamples.forId("1"),
            actions = AppointmentDetailsActions()
        )
    }
}

@Preview(name = "Appointment — awaiting confirmation", showBackground = true, heightDp = 1000)
@Composable
private fun AppointmentAwaitingPreview() {
    MediQTheme {
        AppointmentDetailsContent(
            state = AppointmentDetailsSamples.forId("2"),
            actions = AppointmentDetailsActions()
        )
    }
}

@Preview(name = "Appointment — completed", showBackground = true, heightDp = 1000)
@Composable
private fun AppointmentCompletedPreview() {
    MediQTheme {
        AppointmentDetailsContent(
            state = AppointmentDetailsSamples.forId("3"),
            actions = AppointmentDetailsActions()
        )
    }
}

@Preview(name = "Appointment — loading", showBackground = true, heightDp = 1000)
@Composable
private fun AppointmentLoadingPreview() {
    MediQTheme {
        AppointmentDetailsContent(
            state = AppointmentDetailsSamples.loading,
            actions = AppointmentDetailsActions()
        )
    }
}

@Preview(name = "Appointment — not found", showBackground = true, heightDp = 1000)
@Composable
private fun AppointmentNotFoundPreview() {
    MediQTheme {
        AppointmentDetailsContent(
            state = AppointmentDetailsSamples.forId("missing"),
            actions = AppointmentDetailsActions()
        )
    }
}
