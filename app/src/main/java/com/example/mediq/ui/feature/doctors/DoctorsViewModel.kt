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
 * away. If a request is still running when a new one starts, the old one is
 * cancelled — otherwise a slow early request could land after a fast later one
 * and overwrite the newer results.
 */
class DoctorsViewModel(
    private val doctorRepository: DoctorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorsUiState())
    val uiState: StateFlow<DoctorsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var filterJob: Job? = null

    fun onSearchTextChange(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            loadDoctors()
        }
    }

    fun onSpecialtySelected(specialty: Specialty?) {
        _uiState.value = _uiState.value.copy(selectedSpecialty = specialty)
        filterJob?.cancel()
        filterJob = viewModelScope.launch { loadDoctors() }
    }

    fun refresh() {
        viewModelScope.launch { loadDoctors() }
    }

    private suspend fun loadDoctors() {
        val state = _uiState.value
        _uiState.value = state.copy(doctors = LoadState.Loading)
        _uiState.value = _uiState.value.copy(
            doctors = try {
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
        )
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { DoctorsViewModel(AppContainer.doctorRepository) }
        }
    }
}