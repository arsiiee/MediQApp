package com.example.mediq.ui.screens.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQScaffoldBackground

/** Where the patient came from, handed back by the detail screen. */
private const val SOURCE_TAB_KEY = "appointment_source_tab"

/** What the detail screen sets when a booking is cancelled. */
private const val CANCELLED_ID_KEY = "cancelled_appointment_id"

/**
 * Appointments.
 *
 * Job: track bookings. Primary action: open one booking.
 *
 * [state] defaults to the current mock source, [AppointmentsSamples]. Replacing
 * it with a repository changes this one call site, not the layout.
 */
@Composable
fun AppointmentsScreen(
    navController: NavController,
    state: AppointmentsUiState = AppointmentsSamples.content
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var cancelledIds by remember { mutableStateOf(emptySet<String>()) }

    val selectedTab = AppointmentsTab.entries[selectedTabIndex]

    /*
     * The detail screen talks back through the saved-state handle: which tab an
     * appointment was opened from, and whether it was cancelled. Reading those
     * here keeps the screen's own state the single owner.
     */
    val backStackEntry by navController.currentBackStackEntryAsState()

    val sourceTab = backStackEntry?.savedStateHandle?.get<Int>(SOURCE_TAB_KEY)
    LaunchedEffect(sourceTab) {
        if (sourceTab != null) {
            selectedTabIndex = AppointmentsTab
                .fromIndex(sourceTab)
                ?.ordinal
                ?: selectedTabIndex
            backStackEntry?.savedStateHandle?.remove<Int>(SOURCE_TAB_KEY)
        }
    }

    val cancelledId = backStackEntry?.savedStateHandle?.get<String>(CANCELLED_ID_KEY)
    LaunchedEffect(cancelledId) {
        if (!cancelledId.isNullOrBlank()) {
            cancelledIds = cancelledIds + cancelledId
            // A cancelled booking now lives in history.
            selectedTabIndex = AppointmentsTab.History.ordinal
            backStackEntry?.savedStateHandle?.remove<String>(CANCELLED_ID_KEY)
        }
    }

    AppointmentsContent(
        state = (state as? AppointmentsUiState.Content)?.withCancelled(cancelledIds) ?: state,
        selectedTab = selectedTab,
        onTabSelected = { selectedTabIndex = it.ordinal },
        actions = AppointmentsActions(
            onOpenAppointment = { id, fromTab ->
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set(SOURCE_TAB_KEY, fromTab.ordinal)
                navController.navigate(Screen.AppointmentDetails.createRoute(id))
            }
        )
    )
}

/** State switch. Everything below this point is navigation-free and previewable. */
@Composable
fun AppointmentsContent(
    state: AppointmentsUiState,
    selectedTab: AppointmentsTab,
    onTabSelected: (AppointmentsTab) -> Unit,
    actions: AppointmentsActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQBackHeader(title = "My appointments", onBack = null, height = 80.dp)

        AppointmentTabs(selected = selectedTab, onSelect = onTabSelected)

        when (state) {
            AppointmentsUiState.Loading -> AppointmentsLoadingState()

            is AppointmentsUiState.Error -> AppointmentsErrorState(
                message = state.message,
                onRetry = actions.onRetry
            )

            is AppointmentsUiState.Content -> AppointmentsList(
                tab = selectedTab,
                bookings = state.forTab(selectedTab),
                onOpen = { booking ->
                    actions.onOpenAppointment(booking.id, selectedTab)
                }
            )
        }
    }
}
