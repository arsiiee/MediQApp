package com.example.mediq.ui.screens.booking

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
 * Booking flow.
 *
 * Job: confirm one slot.
 * Primary action: "Confirm Booking".
 *
 * [submission] defaults to the current mock source, [BookingSamples]. Swapping
 * it for a repository changes this one call site, not the layout.
 */
@Composable
fun BookingFlowScreen(
    navController: NavController,
    doctorId: String?,
    dateIndex: Int,
    selectedTime: String,
    submission: BookingSubmission = BookingSamples.idle
) {
    BookingFlowContent(
        summary = BookingSamples.summary(
            dateIndex = dateIndex,
            time = selectedTime
        ),
        submission = submission,
        actions = BookingActions(
            onBack = { navController.popBackStack() },
            onConfirm = {
                navController.navigate(
                    Screen.BookingSuccess.createRoute(
                        dateIndex = dateIndex,
                        time = selectedTime
                    )
                )
            },
            onChooseAnotherSlot = { navController.popBackStack() }
        )
    )
}

/** State switch for the form. Everything below is navigation-free. */
@Composable
fun BookingFlowContent(
    summary: BookingSummary,
    submission: BookingSubmission,
    actions: BookingActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQBackHeader(
            title = "Book Appointment",
            onBack = actions.onBack
        )

        BookingForm(
            summary = summary,
            submission = submission,
            actions = actions
        )
    }
}

/**
 * Booking result.
 *
 * Job: say what happened to the booking. [state] defaults to the mock source.
 */
@Composable
fun BookingSuccessScreen(
    navController: NavController,
    dateIndex: Int,
    selectedTime: String,
    state: BookingSuccessState = BookingSamples.success(
        dateIndex = dateIndex,
        time = selectedTime
    )
) {
    BookingSuccessContent(
        state = state,
        actions = BookingActions(
            onChooseAnotherSlot = { navController.popBackStack() },
            onViewAppointments = {
                navController.navigate(Screen.Appointments.route) {
                    popUpTo(Screen.Home.route) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        )
    )
}

/** State switch for the result. No back control: this screen ends the flow. */
@Composable
fun BookingSuccessContent(
    state: BookingSuccessState,
    actions: BookingActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQBackHeader(
            title = when (state) {
                is BookingSuccessState.Success -> "Booking Successful"
                is BookingSuccessState.Conflict -> "Booking Not Completed"
            },
            onBack = null
        )

        when (state) {
            is BookingSuccessState.Success -> BookingSuccessBody(
                summary = state.summary,
                actions = actions
            )
            is BookingSuccessState.Conflict -> BookingConflictBody(
                message = state.message,
                actions = actions
            )
        }
    }
}
