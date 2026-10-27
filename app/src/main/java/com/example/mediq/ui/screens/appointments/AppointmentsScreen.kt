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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.screens.appointments.AppointmentCancellationStore
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.viewmodel.AppointmentsViewModel

/** Where the patient came from, handed back by the detail screen. */
private const val SOURCE_TAB_KEY = "appointment_source_tab"

/** What the detail screen sets when a booking is cancelled. */
private const val CANCELLED_ID_KEY = "cancelled_appointment_id"

/**
 * Appointments.
 *
 * Job: track bookings. Primary action: open one booking.
 *
 * Bookings are loaded from the API via [AppointmentsViewModel]; sample data is
 * no longer used in the running app.
 */
@Composable
fun AppointmentsScreen(
    navController: NavController,
    viewModel: AppointmentsViewModel = viewModel()
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var cancelledIds by rememberSaveable { mutableStateOf(emptySet<String>()) }

    val bookings by viewModel.appointments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadAppointments()
    }

    val selectedTab = AppointmentsTab.entries[selectedTabIndex]

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

    val all = bookings
    val upcoming = all.filter { it.status.canCancel }
    val history = all.filterNot { it.status.canCancel }
    val content = AppointmentsUiState.Content(upcoming = upcoming, history = history).withCancelled(cancelledIds)

    val state = when {
        isLoading -> AppointmentsUiState.Loading
        error != null -> AppointmentsUiState.Error(error.orEmpty())
        else -> content
    }

    AppointmentsContent(
        state = state,
        selectedTab = selectedTab,
        onTabSelected = { selectedTabIndex = it.ordinal },
        actions = AppointmentsActions(
            onRetry = { viewModel.loadAppointments() },
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
        MediQPageHeader(
            title = "My appointments",
            subtitle = "Upcoming and past visits"
        )

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
