package com.example.mediq.ui.screens.appointmentdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.screens.appointments.AppointmentCancellationStore
import com.example.mediq.ui.screens.appointments.AppointmentStatus
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.viewmodel.AppointmentsViewModel
import kotlinx.coroutines.launch

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

    val scope = rememberCoroutineScope()

    LaunchedEffect(appointmentId) {
        if (appointmentId != null) {
            viewModel.loadAppointment(appointmentId)
        }
    }

    val state = when {
        isLoading && booking == null -> AppointmentDetailsUiState.Loading
        error != null -> AppointmentDetailsUiState.Error(error.orEmpty())
        booking != null -> {
            val current = booking!!
            val cancelled = cancelledIds.contains(appointmentId ?: "") || current.status == AppointmentStatus.Cancelled
            if (cancelled && appointmentId != null) {
                AppointmentDetailsUiState.Content(current.copy(status = AppointmentStatus.Cancelled))
            } else {
                AppointmentDetailsUiState.Content(current)
            }
        }
        else -> AppointmentDetailsUiState.Error("This appointment is no longer in your records.")
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    AppointmentDetailsContent(
        state = state,
        actions = AppointmentDetailsActions(
            onBack = { navController.popBackStack() },
            onRetry = {
                if (appointmentId != null) {
                    viewModel.loadAppointment(appointmentId)
                }
            },
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
                scope.launch {
                    android.util.Log.d("MediQBookingDiag", "UI cancel requested id=$id reasonLen=${reason.length}")
                    val ok = viewModel.cancelAppointment(id, reason)
                    if (ok) {
                        android.widget.Toast.makeText(context, "Appointment cancelled", android.widget.Toast.LENGTH_SHORT).show()
                        AppointmentCancellationStore.cancel(id)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(CANCELLED_ID_KEY, id)
                        navController.popBackStack()
                    } else {
                        val msg = viewModel.error.value ?: "Failed to cancel appointment"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
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
