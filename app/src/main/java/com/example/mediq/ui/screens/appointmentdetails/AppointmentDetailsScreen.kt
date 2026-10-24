package com.example.mediq.ui.screens.appointmentdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQScaffoldBackground

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
 * [state] defaults to the current mock source, [AppointmentDetailsSamples].
 * Replacing it with a repository changes this one call site, not the layout.
 */
@Composable
fun AppointmentDetailsScreen(
    navController: NavController,
    appointmentId: String?,
    state: AppointmentDetailsUiState = AppointmentDetailsSamples.forId(appointmentId)
) {
    AppointmentDetailsContent(
        state = state,
        actions = AppointmentDetailsActions(
            onBack = { navController.popBackStack() },
            onChatWithSecretary = { conversationId ->
                navController.navigate(Screen.MessageChat.createRoute(conversationId))
            },
            onCancelAppointment = { id ->
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
    actions: AppointmentDetailsActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQBackHeader(
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
