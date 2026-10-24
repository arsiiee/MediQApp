package com.example.mediq.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.session.MediQSession
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing

/** Height of the green band behind the header. */
private val HeaderHeight = MediQSize.homeHeader

/** How far the next-consultation card overlaps the green band. */
private val CardTopOffset = MediQSize.homeCardOffset

/**
 * Home.
 *
 * Job: orient the patient — show the next appointment, then get them booking.
 * Primary action: "Book a consultation".
 *
 * [state] defaults to the current mock source, [HomeSamples]. Swapping that for
 * a real data source changes this one call site, not the layout.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    state: HomeUiState = HomeSamples.content
) {
    var notificationsRead by rememberSaveable { mutableStateOf(false) }
    val user by MediQSession.user.collectAsState()
    val signedInState = state.withPatient(user)
    val resolvedState = if (signedInState is HomeUiState.Content && notificationsRead) {
        signedInState.copy(unreadNotifications = 0)
    } else {
        signedInState
    }
    HomeContent(
        state = resolvedState,
        actions = HomeActions(
            onNotificationsClick = {
                notificationsRead = true
                navController.navigate(Screen.Notifications.route)
            },
            onBrowseDoctors = {
                navController.navigate(Screen.Doctors.route)
            },
            onDoctorClick = { doctorId ->
                navController.navigate(Screen.DoctorDetails.createRoute(doctorId))
            },
            onManageAppointment = { appointmentId ->
                navController.navigate(Screen.AppointmentDetails.createRoute(appointmentId))
            }
        )
    )
}

/** State switch. Everything below this point is navigation-free and previewable. */
@Composable
fun HomeContent(
    state: HomeUiState,
    actions: HomeActions
) {
    when (state) {
        is HomeUiState.Content -> HomeLoaded(state = state, actions = actions)
        HomeUiState.Loading -> HomeLoadingState()
        is HomeUiState.Error -> HomeErrorState(
            message = state.message,
            onRetry = actions.onRetry
        )
    }
}

@Composable
private fun HomeLoaded(
    state: HomeUiState.Content,
    actions: HomeActions
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQBackground),
        contentPadding = PaddingValues(bottom = MediQSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.lg)
    ) {

        item {
            Box(modifier = Modifier.fillMaxWidth()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HeaderHeight)
                        .background(MediQGreen)
                        .align(Alignment.TopCenter)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = CardTopOffset,
                            start = MediQSpacing.gutter,
                            end = MediQSpacing.gutter
                        )
                ) {
                    val appointment = state.nextAppointment
                    if (appointment == null) {
                        NoUpcomingAppointmentCard()
                    } else {
                        NextConsultationCard(
                            appointment = appointment,
                            onManageAppointment = actions.onManageAppointment
                        )
                    }
                }

                HomeHeader(
                    patient = state.patient,
                    unreadNotifications = state.unreadNotifications,
                    onNotificationsClick = actions.onNotificationsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MediQSpacing.gutter)
            ) {
                BookConsultationButton(onClick = actions.onBrowseDoctors)
            }
        }

        item {
            SpecialtyChips(
                specialties = state.specialties,
                onSpecialtyClick = { actions.onBrowseDoctors() }
            )
        }

        item {
            OpenSlotsCard(
                doctors = state.openSlots,
                onDoctorClick = actions.onDoctorClick,
                modifier = Modifier.padding(horizontal = MediQSpacing.gutter)
            )
        }
    }
}

