package com.example.mediq.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.data.api.SupabaseApiService
import com.example.mediq.data.model.BookingRequest
import com.example.mediq.data.model.Doctor
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.session.DateTimeFormatter
import com.example.mediq.ui.session.MediQSession
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.viewmodel.BookingViewModel
import kotlinx.coroutines.launch

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
    date: String,
    selectedTime: String,
    viewModel: BookingViewModel = viewModel()
) {
    var reason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Real doctor info fetched from the API (not mock data)
    var doctorName by remember { mutableStateOf<String?>(null) }
    var specialty by remember { mutableStateOf<String?>(null) }
    var location by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val bookingResult by viewModel.bookingResult.collectAsState()

    val submission = when {
        isLoading -> BookingSubmission.Submitting
        error != null -> BookingSubmission.Error(error ?: "Booking failed")
        else -> BookingSubmission.Idle
    }

    // Fetch real doctor info for display instead of mock samples
    LaunchedEffect(doctorId) {
        if (doctorId != null) {
            scope.launch {
                try {
                    val doc = SupabaseApiService().getDoctor(doctorId)
                    doctorName = doc.fullName
                    specialty = doc.specialty
                    location =
                        "${doc.location.building} — ${doc.location.floor}F · ${doc.location.room}"
                } catch (e: Exception) {
                    // fall back to mock samples below
                }
            }
        }
    }

    // Format the real date from the schedule for display
    val startsAt = time12hToIso(date, selectedTime)
    val dateLabel = DateTimeFormatter.formatDateLong(startsAt)

    val summary = BookingSummary(
        doctorName = doctorName ?: "Doctor",
        specialty = specialty ?: "",
        location = location ?: "",
        dateLabel = dateLabel,
        time = selectedTime
    )

    BookingFlowContent(
        summary = summary,
        submission = submission,
        actions = BookingActions(
            onBack = { navController.popBackStack() },
            onConfirm = {
                val patientId = MediQSession.authUserId
                    ?: throw RuntimeException("Not signed in")
                val startsAt = time12hToIso(date, selectedTime)
                viewModel.bookAppointment(
                    BookingRequest(
                        patientId = patientId,
                        doctorId = doctorId
                            ?: throw RuntimeException("Doctor id missing — cannot book without a doctor"),
                        startsAt = startsAt,
                        durationMinutes = 30,
                        reason = reason
                    )
                )
            },
            onChooseAnotherSlot = { navController.popBackStack() }
        ),
        reason = reason,
        onReasonChange = { reason = it }
    )

    // Navigate to the success screen once the booking lands.
    LaunchedEffect(bookingResult) {
        if (bookingResult != null) {
            viewModel.clearBookingResult()
            navController.navigate(
                Screen.BookingSuccess.createRoute(
                    date = date,
                    time = selectedTime,
                    doctorName = doctorName ?: "Doctor",
                    specialty = specialty ?: "",
                    location = location ?: "",
                    dateLabel = dateLabel
                )
            )
        }
    }
}

/** "2026-09-21" + "9:00 AM" -> "2026-09-21T09:00:00". */
private fun time12hToIso(date: String, time12h: String): String {
    val (timePart, amPm) = time12h.split(" ")
    val (hourStr, minuteStr) = timePart.split(":")
    var h = hourStr.toInt()
    val m = minuteStr.toInt()
    if (amPm == "PM" && h != 12) h += 12
    if (amPm == "AM" && h == 12) h = 0
    return "${date}T${"%02d".format(h)}:${"%02d".format(m)}:00"
}

/** State switch for the form. Everything below is navigation-free. */
@Composable
fun BookingFlowContent(
    summary: BookingSummary,
    submission: BookingSubmission,
    actions: BookingActions,
    reason: String = "",
    onReasonChange: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQPageHeader(
            title = "Book Appointment",
            onBack = actions.onBack
        )

        BookingForm(
            summary = summary,
            submission = submission,
            actions = actions,
            reason = reason,
            onReasonChange = onReasonChange
        )
    }
}

/**
 * Booking result.
 *
 * Job: say what happened to the booking. Uses real doctor info passed from
 * the booking flow instead of mock samples.
 */
@Composable
fun BookingSuccessScreen(
    navController: NavController,
    date: String,
    selectedTime: String,
    doctorName: String,
    specialty: String,
    location: String,
    dateLabel: String,
    state: BookingSuccessState = BookingSuccessState.Success(
        BookingSummary(
            doctorName = doctorName,
            specialty = specialty,
            location = location,
            dateLabel = dateLabel,
            time = selectedTime
        )
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
        MediQPageHeader(
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
