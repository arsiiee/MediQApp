package com.example.mediq.ui.screens.doctordetails

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
 * Doctor details.
 *
 * Job: decide on this one doctor and pick a slot.
 * Primary action: "Continue" with a time selected.
 *
 * [state] defaults to the current mock source, [DoctorDetailsSamples]. Swapping
 * that for a repository changes this one call site, not the layout.
 */
@Composable
fun DoctorDetailsScreen(
    navController: NavController,
    doctorId: String?,
    state: DoctorDetailsUiState = DoctorDetailsSamples.content
) {
    DoctorDetailsContent(
        state = state,
        actions = DoctorDetailsActions(
            onBack = { navController.popBackStack() },
            onContinue = { dateIndex, time ->
                navController.navigate(
                    Screen.BookingFlow.createRoute(
                        doctorId = doctorId ?: "1",
                        dateIndex = dateIndex,
                        time = time
                    )
                )
            }
        )
    )
}

/**
 * State switch. Everything below this point is navigation-free and previewable.
 *
 * The header sits above every state: the patient can always get back out, even
 * from a loading or error screen.
 */
@Composable
fun DoctorDetailsContent(
    state: DoctorDetailsUiState,
    actions: DoctorDetailsActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQBackHeader(
            title = "Doctors Profile",
            onBack = actions.onBack
        )

        when (state) {
            DoctorDetailsUiState.Loading -> DoctorDetailsLoadingState()

            is DoctorDetailsUiState.Error -> DoctorDetailsErrorState(
                message = state.message,
                onRetry = actions.onRetry
            )

            is DoctorDetailsUiState.NoSlots -> DoctorDetailsNoSlotsState(
                doctor = state.doctor,
                onBack = actions.onBack
            )

            is DoctorDetailsUiState.Content ->
                if (state.hasOpenSlots) {
                    DoctorDetailsLoaded(state = state, actions = actions)
                } else {
                    // The data arrived and says nothing is bookable: same
                    // message as [DoctorDetailsUiState.NoSlots], no reload.
                    DoctorDetailsNoSlotsState(
                        doctor = state.doctor,
                        onBack = actions.onBack
                    )
                }
        }
    }
}
