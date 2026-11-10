package com.example.mediq.ui.screens.appointmentdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.screens.appointments.AppointmentCancellationStore
import com.example.mediq.ui.screens.appointments.AppointmentStatus
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.viewmodel.AppointmentsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * What the list reads back when a booking is cancelled here. Kept identical to
 * `AppointmentsScreen`'s key; the two screens share this one contract.
 */
internal const val CANCELLED_ID_KEY = "cancelled_appointment_id"

/**
 * Appointment details.
 *
 * Job: manage one booking. Primary action: chat with the secretary, or cancel.
 *
 * The booking is loaded from the API via [AppointmentsViewModel]; sample data
 * is no longer used in the running app.
 */
@Composable
fun AppointmentDetailsScreen(
    navController: NavController,
    appointmentId: String?,
    viewModel: AppointmentsViewModel = viewModel()
) {
    val booking by viewModel.appointmentDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val cancelledIds by AppointmentCancellationStore.cancelledIds.collectAsState()

    LaunchedEffect(appointmentId) {
        if (appointmentId != null) {
            viewModel.loadAppointment(appointmentId)
        }
    }

    val state = when {
        isLoading && booking == null -> AppointmentDetailsUiState.Loading
        error != null && booking == null -> AppointmentDetailsUiState.Error(error.orEmpty())
        booking != null -> {
            booking?.let { current ->
                val cancelled = cancelledIds.contains(appointmentId ?: "") || current.status == AppointmentStatus.Cancelled
                if (cancelled && appointmentId != null) {
                    AppointmentDetailsUiState.Content(current.copy(status = AppointmentStatus.Cancelled))
                } else {
                    AppointmentDetailsUiState.Content(current)
                }
            } ?: AppointmentDetailsUiState.Error("This appointment is no longer in your records.")
        }
        else -> AppointmentDetailsUiState.Error("This appointment is no longer in your records.")
    }

    AppointmentDetailsContent(
        state = state,
        actions = AppointmentDetailsActions(
            onBack = { navController.popBackStack() },
            onChatWithSecretary = { conversationId ->
                navController.navigate(Screen.MessageChat.createRoute(conversationId))
            },
            onCancelAppointment = { id ->
                AppointmentCancellationStore.cancel(id)
                /*
                 * Hand the cancellation to the list rather than moving it here:
                 * the list owns where each booking sits. Set it on the entry
                 * the patient is about to return to, then pop back to it.
                 */
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(CANCELLED_ID_KEY, id)

                navController.popBackStack()
            }
        )
    )
}

/** State switch. Everything below this point is navigation-free and previewable. */
@Composable
fun AppointmentDetailsContent(
    state: AppointmentDetailsUiState,
    actions: AppointmentDetailsActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQPageHeader(
            title = "Appointment Details",
            onBack = actions.onBack
        )

        when (state) {
            AppointmentDetailsUiState.Loading -> AppointmentDetailsLoadingState()

            is AppointmentDetailsUiState.Error -> AppointmentDetailsErrorState(
                message = state.message,
                onRetry = actions.onRetry
            )

            is AppointmentDetailsUiState.Content -> AppointmentDetailsLoaded(
                booking = state.booking,
                actions = actions
            )
        }
    }
}
