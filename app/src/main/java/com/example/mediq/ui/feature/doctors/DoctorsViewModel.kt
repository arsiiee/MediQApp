package com.example.mediq.ui.feature.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.repository.DoctorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DoctorsUiState(
    val searchText: String = "",
    val selectedSpecialty: Specialty? = null,
    val doctors: LoadState<List<Doctor>> = LoadState.Loading,
)

/**
 * Holds the search text and the chosen specialty, and re-queries whenever one
 * of them changes.
 *
 * Typing restarts a single debounce timer; changing the filter queries straight
 * away. A newer query invalidates older results, including the opening read:
 * cancelling the search or filter job alone cannot cancel an `init` request.
 */
class DoctorsViewModel(
    private val doctorRepository: DoctorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorsUiState())
    val uiState: StateFlow<DoctorsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var filterJob: Job? = null
    private var requestVersion = 0L

    fun onSearchTextChange(text: String) {
        val version = ++requestVersion
        _uiState.value = _uiState.value.copy(searchText = text, doctors = LoadState.Loading)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            if (version == requestVersion) loadDoctors()
        }
    }

    fun onSpecialtySelected(specialty: Specialty?) {
        ++requestVersion
        _uiState.value = _uiState.value.copy(selectedSpecialty = specialty, doctors = LoadState.Loading)
        filterJob?.cancel()
        filterJob = viewModelScope.launch { loadDoctors() }
    }

    fun refresh() {
        viewModelScope.launch { loadDoctors() }
    }

    init {
        // The list is loaded when the screen opens, not when the patient types.
        //
        // This was missing until 2026-10-07, and it shipped with a green build,
        // a green suite, and a test that asserted the absence as intended — see
        // "Opening the screen fetches" in `DoctorsViewModelTest`. `doctors` starts
        // as `LoadState.Loading` (`DoctorsUiState`, line 26) and `onSearchTextChange`
        // is the only thing that used to move it, so a patient who opened the
        // Doctors tab saw a spinner forever unless they typed in the search box.
        // Verified on a device: four taps over ninety seconds produced zero
        // `/doctors` requests in logcat; one keystroke produced the request
        // immediately.
        //
        // `refresh()` rather than `loadDoctors()` directly: it is the same
        // `viewModelScope.launch { loadDoctors() }` the Re-check affordance uses,
        // so there is one way to start a read rather than two.
        refresh()
    }

    private suspend fun loadDoctors() {
        val version = ++requestVersion
        val state = _uiState.value
        _uiState.value = state.copy(doctors = LoadState.Loading)
        val result = try {
            val query = DoctorQuery(
                searchText = state.searchText.takeIf { it.isNotBlank() },
                specialty = state.selectedSpecialty,
            )
            LoadState.Success(doctorRepository.getDoctors(query).items)
        } catch (e: BackendNotConnectedException) {
            // Expected until a backend exists. An empty list, not an error.
            LoadState.Success(emptyList())
        } catch (e: ApiFailure) {
            LoadState.Error(e.message)
        } catch (e: Exception) {
            LoadState.Error("Couldn't load doctors. Try again in a moment.")
        }
        if (version == requestVersion) {
            _uiState.value = _uiState.value.copy(doctors = result)
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { DoctorsViewModel(AppContainer.doctorRepository) }
        }
    }
}