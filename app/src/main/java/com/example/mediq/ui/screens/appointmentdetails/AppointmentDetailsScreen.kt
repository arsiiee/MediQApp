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

private const val CANCELLED_ID_KEY = "cancelled_appointment_id"

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

    val doctorName = booking?.doctor

    LaunchedEffect(appointmentId) {
        if (appointmentId != null) {
            viewModel.loadAppointment(appointmentId)
        }
    }

    val state = when {
        isLoading && booking == null -> AppointmentDetailsUiState.Loading
        error != null && booking == null -> AppointmentDetailsUiState.Error(error.orEmpty())
        booking != null -> {
            val current = booking!!
            val cancelled = cancelledIds.contains(appointmentId ?: "") || current.status == AppointmentStatus.Cancelled
            if (cancelled && appointmentId != null) {
                AppointmentDetailsUiState.Content(current.copy(status = AppointmentStatus.Cancelled))
            } else {
                AppointmentDetailsUiState.Content(current)
            }
        } else -> AppointmentDetailsUiState.Error("This appointment is no longer in your records.")
    }

    AppointmentDetailsContent(
        state = state,
        actions = AppointmentDetailsActions(
            onBack = { navController.popBackStack() },
            onChatWithSecretary = { conversationId ->
                if (booking?.status != AppointmentStatus.Cancelled) {
                    val appointmentTime = booking?.let { b ->
                        "${b.dateLong} at ${b.time}"
                    } ?: ""
                    navController.navigate(
                        Screen.MessageChat.createRoute(
                            conversationId,
                            "${doctorName ?: "Doctor"}'s Office",
                            doctorName ?: "Doctor",
                            "",
                            appointmentTime,
                        )
                    )
                }
            },
            onCancelAppointment = { id, reason ->
                viewModel.cancelAppointment(id, reason)
                AppointmentCancellationStore.cancel(id)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(CANCELLED_ID_KEY, id)

                navController.popBackStack()
            }
        )
    )
}

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
