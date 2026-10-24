package com.example.mediq.ui.screens.doctordetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

/*
 * Every doctor-details state, renderable without a device.
 *
 * The two "nothing to book" cases are separate previews on purpose: one is a
 * single full day, the other is a doctor with no availability at all, and the
 * screen says something different for each.
 */

@Preview(name = "Details — loaded", showBackground = true, heightDp = 1400)
@Composable
private fun DoctorDetailsLoadedPreview() {
    MediQTheme {
        DoctorDetailsContent(
            state = DoctorDetailsSamples.content,
            actions = DoctorDetailsActions()
        )
    }
}

@Preview(name = "Details — loading", showBackground = true, heightDp = 900)
@Composable
private fun DoctorDetailsLoadingPreview() {
    MediQTheme {
        DoctorDetailsContent(
            state = DoctorDetailsSamples.loading,
            actions = DoctorDetailsActions()
        )
    }
}

@Preview(name = "Details — error", showBackground = true, heightDp = 900)
@Composable
private fun DoctorDetailsErrorPreview() {
    MediQTheme {
        DoctorDetailsContent(
            state = DoctorDetailsSamples.error,
            actions = DoctorDetailsActions()
        )
    }
}

@Preview(name = "Details — no slots at all", showBackground = true, heightDp = 1400)
@Composable
private fun DoctorDetailsNoSlotsPreview() {
    MediQTheme {
        DoctorDetailsContent(
            state = DoctorDetailsSamples.noSlots,
            actions = DoctorDetailsActions()
        )
    }
}

@Preview(name = "Details — one full day", showBackground = true, heightDp = 1400)
@Composable
private fun DoctorDetailsFullDayPreview() {
    MediQTheme {
        DoctorDetailsContent(
            state = DoctorDetailsUiState.Content(
                doctor = DoctorDetailsSamples.profile,
                schedule = DoctorDetailsSamples.scheduleWithNothingOpen()
            ),
            actions = DoctorDetailsActions()
        )
    }
}
