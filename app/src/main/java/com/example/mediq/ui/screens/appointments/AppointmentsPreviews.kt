package com.example.mediq.ui.screens.appointments

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

/*
 * Every appointments state, renderable without a device.
 *
 * The two empty cases are separate previews on purpose: nothing booked yet is
 * a different message from nothing left to come.
 */

@Preview(name = "Appointments — upcoming", showBackground = true, heightDp = 900)
@Composable
private fun AppointmentsUpcomingPreview() {
    MediQTheme {
        AppointmentsContent(
            state = AppointmentsSamples.content,
            selectedTab = AppointmentsTab.Upcoming,
            onTabSelected = {},
            actions = AppointmentsActions()
        )
    }
}

@Preview(name = "Appointments — history", showBackground = true, heightDp = 900)
@Composable
private fun AppointmentsHistoryPreview() {
    MediQTheme {
        AppointmentsContent(
            state = AppointmentsSamples.content,
            selectedTab = AppointmentsTab.History,
            onTabSelected = {},
            actions = AppointmentsActions()
        )
    }
}

@Preview(name = "Appointments — nothing booked", showBackground = true, heightDp = 900)
@Composable
private fun AppointmentsEmptyPreview() {
    MediQTheme {
        AppointmentsContent(
            state = AppointmentsSamples.empty,
            selectedTab = AppointmentsTab.Upcoming,
            onTabSelected = {},
            actions = AppointmentsActions()
        )
    }
}

@Preview(name = "Appointments — no upcoming visits", showBackground = true, heightDp = 900)
@Composable
private fun AppointmentsPastOnlyPreview() {
    MediQTheme {
        AppointmentsContent(
            state = AppointmentsSamples.pastOnly,
            selectedTab = AppointmentsTab.Upcoming,
            onTabSelected = {},
            actions = AppointmentsActions()
        )
    }
}

@Preview(name = "Appointments — loading", showBackground = true, heightDp = 900)
@Composable
private fun AppointmentsLoadingPreview() {
    MediQTheme {
        AppointmentsContent(
            state = AppointmentsSamples.loading,
            selectedTab = AppointmentsTab.Upcoming,
            onTabSelected = {},
            actions = AppointmentsActions()
        )
    }
}

@Preview(name = "Appointments — error", showBackground = true, heightDp = 900)
@Composable
private fun AppointmentsErrorPreview() {
    MediQTheme {
        AppointmentsContent(
            state = AppointmentsSamples.error,
            selectedTab = AppointmentsTab.Upcoming,
            onTabSelected = {},
            actions = AppointmentsActions()
        )
    }
}
