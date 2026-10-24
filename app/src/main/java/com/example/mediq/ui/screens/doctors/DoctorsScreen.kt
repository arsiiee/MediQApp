package com.example.mediq.ui.screens.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQDivider
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSpacing

/**
 * Doctors.
 *
 * Job: find one doctor and open their details.
 * Primary action: opening a result row.
 *
 * [state] defaults to the current mock source, [DoctorsSamples]. Replacing it
 * with a repository changes this one call site, not the layout.
 */
@Composable
fun DoctorsScreen(
    navController: NavController,
    state: DoctorsUiState = DoctorsSamples.loaded
) {
    DoctorsContent(
        state = state,
        actions = DoctorsActions(
            onDoctorClick = { doctorId ->
                navController.navigate(Screen.DoctorDetails.createRoute(doctorId))
            }
        )
    )
}

/** State switch. Everything below this point is navigation-free and previewable. */
@Composable
fun DoctorsContent(
    state: DoctorsUiState,
    actions: DoctorsActions
) {
    when (state) {
        DoctorsUiState.Loading -> DoctorsLoadingState()
        is DoctorsUiState.Error -> DoctorsErrorState(
            message = state.message,
            onRetry = actions.onRetry
        )
        is DoctorsUiState.Content -> DoctorsResults(
            doctors = state.doctors,
            actions = actions
        )
    }
}

@Composable
private fun DoctorsResults(
    doctors: List<DoctorInfo>,
    actions: DoctorsActions
) {
    var filters by remember { mutableStateOf(DoctorFilters.Default) }
    var specialtyMenuExpanded by remember { mutableStateOf(false) }
    var locationMenuExpanded by remember { mutableStateOf(false) }

    val visibleDoctors = remember(doctors, filters) {
        filterDoctors(doctors = doctors, filters = filters)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground),
        contentPadding = PaddingValues(
            start = MediQSpacing.gutter,
            end = MediQSpacing.gutter,
            top = MediQSpacing.lg,
            bottom = MediQSpacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.md)
    ) {

        item {
            DoctorsSearchControls(
                filters = filters,
                onFiltersChange = { filters = it },
                specialtyMenuExpanded = specialtyMenuExpanded,
                onSpecialtyMenuExpandedChange = { expanded ->
                    specialtyMenuExpanded = expanded
                    // Only one menu is open at a time.
                    if (expanded) locationMenuExpanded = false
                },
                locationMenuExpanded = locationMenuExpanded,
                onLocationMenuExpandedChange = { expanded ->
                    locationMenuExpanded = expanded
                    if (expanded) specialtyMenuExpanded = false
                },
                resultCount = visibleDoctors.size
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MediQDivider
            )
        }

        if (doctors.isEmpty()) {
            item { DoctorsEmptyState() }
        } else if (visibleDoctors.isEmpty()) {
            item {
                DoctorsNoMatchesState(
                    onClearFilters = { filters = DoctorFilters.Default }
                )
            }
        } else {
            items(items = visibleDoctors, key = { it.id }) { doctor ->
                DoctorListItem(
                    doctor = doctor,
                    onClick = { actions.onDoctorClick(doctor.id) }
                )
            }
        }
    }
}
