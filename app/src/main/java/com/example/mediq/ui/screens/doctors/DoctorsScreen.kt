package com.example.mediq.ui.screens.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.theme.MediQDivider
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.viewmodel.DoctorsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Doctors.
 *
 * Job: find one doctor and open their details.
 * Primary action: opening a result row.
 *
 * Doctors are loaded from the server via [DoctorsViewModel]. The sample data in
 * [DoctorSamples] is kept in the project but no longer used by this screen.
 */
@Composable
fun DoctorsScreen(
    navController: NavController,
    viewModel: DoctorsViewModel = viewModel()
) {
    val doctors by viewModel.doctors.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val specializations by viewModel.specializations.collectAsState()

    var filters by remember { mutableStateOf(DoctorFilters.Default) }

    // Load specializations once so we can map specialty names to specializationId.
    LaunchedEffect(Unit) {
        viewModel.loadSpecializations()
    }

    // Derive specializationId from the currently selected specialty name.
    val specializationId = specializations.firstOrNull { it.name == filters.specialty }?.id
    val searchQuery = filters.query.takeIf { it.isNotBlank() }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Load doctors immediately when the specialty filter changes.
    LaunchedEffect(specializationId) {
        searchJob?.cancel()
        viewModel.loadDoctors(specializationId = specializationId, search = searchQuery)
    }

    // Debounce the search query so typing doesn't hit the server on every keystroke.
    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        searchJob = launch {
            delay(300)
            val currentQuery = filters.query.takeIf { it.isNotBlank() }
            if (currentQuery == searchQuery) {
                viewModel.loadDoctors(specializationId = specializationId, search = searchQuery)
            }
        }
    }

    val uiState = when {
        isLoading -> DoctorsUiState.Loading
        error != null -> DoctorsUiState.Error(error ?: "Failed to load doctors")
        else -> DoctorsUiState.Content(
            doctors = doctors.map { doctor ->
                mapDoctorToDoctorInfo(doctor, specializations)
            }
        )
    }

    DoctorsContent(
        state = uiState,
        filters = filters,
        onFiltersChange = { filters = it },
        actions = DoctorsActions(
            onDoctorClick = { doctorId ->
                navController.navigate(Screen.DoctorDetails.createRoute(doctorId))
            },
            onRetry = {
                viewModel.loadDoctors(
                    specializationId = specializationId,
                    search = searchQuery
                )
            }
        )
    )
}

/** Maps a server [Doctor] to the screen's [DoctorInfo] view model. */
private fun mapDoctorToDoctorInfo(
    doctor: com.example.mediq.data.model.Doctor,
    specializations: List<com.example.mediq.data.model.Specialty>
): DoctorInfo {
    val initials = doctor.fullName
        .removePrefix("Dr. ")
        .split(" ")
        .filter { it.isNotEmpty() }
        .map { it.first() }
        .joinToString("")
        .uppercase()

    val location = "${doctor.location.building} — ${doctor.location.floor}F · Room ${doctor.location.room}"

    return DoctorInfo(
        id = doctor.id,
        name = doctor.fullName,
        initials = initials,
        specialty = doctor.specialty,
        experience = doctor.yearsOfExperience,
        fee = (doctor.feeCentavos / 100).toInt(),
        location = location,
        openSlots = 0,
        licenseNumber = doctor.licenseNumber.takeIf { it.isNotEmpty() }
    )
}

/** State switch. Everything below this point is navigation-free and previewable. */
@Composable
fun DoctorsContent(
    state: DoctorsUiState,
    filters: DoctorFilters,
    onFiltersChange: (DoctorFilters) -> Unit,
    actions: DoctorsActions
) {
    when (state) {
        DoctorsUiState.Loading -> DoctorsLoadingState()
        is DoctorsUiState.Error -> DoctorsErrorState(
            message = state.message,
            onRetry = actions.onRetry
        )
        is DoctorsUiState.Content -> DoctorsScreenContent(
            doctors = state.doctors,
            filters = filters,
            onFiltersChange = onFiltersChange,
            actions = actions
        )
    }
}

/** Doctors list with header, rendered when content is loaded. */
@Composable
private fun DoctorsScreenContent(
    doctors: List<DoctorInfo>,
    filters: DoctorFilters,
    onFiltersChange: (DoctorFilters) -> Unit,
    actions: DoctorsActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQPageHeader(title = "Doctors")

        DoctorsResults(
            doctors = doctors,
            filters = filters,
            onFiltersChange = onFiltersChange,
            actions = actions,
            contentModifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DoctorsResults(
    doctors: List<DoctorInfo>,
    filters: DoctorFilters,
    onFiltersChange: (DoctorFilters) -> Unit,
    actions: DoctorsActions,
    contentModifier: Modifier = Modifier
) {
    var specialtyMenuExpanded by remember { mutableStateOf(false) }
    var locationMenuExpanded by remember { mutableStateOf(false) }

    val visibleDoctors = remember(doctors, filters) {
        filterDoctors(doctors = doctors, filters = filters)
    }

    LazyColumn(
        modifier = contentModifier
            .fillMaxWidth()
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
                onFiltersChange = onFiltersChange,
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
                    onClearFilters = { onFiltersChange(DoctorFilters.Default) }
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
