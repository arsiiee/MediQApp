package com.example.mediq.ui.feature.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppointmentsUiState(
    val selectedTab: Int = 0,
    val upcoming: LoadState<List<Appointment>> = LoadState.Loading,
    val history: LoadState<List<Appointment>> = LoadState.Loading,
) {
    val current: LoadState<List<Appointment>>
        get() = if (selectedTab == 0) upcoming else history
}

/**
 * Both tabs are loaded up front and kept separate, so switching tabs shows the
 * result immediately instead of showing a spinner each time.
 */
class AppointmentsViewModel(
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    init {
        load(AppointmentFilter.UPCOMING)
        load(AppointmentFilter.HISTORY)
    }

    fun onTabSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun refresh() {
        load(AppointmentFilter.UPCOMING)
        load(AppointmentFilter.HISTORY)
    }

    private fun load(filter: AppointmentFilter) {
        viewModelScope.launch {
            val result = try {
                LoadState.Success(appointmentRepository.getAppointments(filter).items)
            } catch (e: BackendNotConnectedException) {
                LoadState.Success(emptyList())
            } catch (e: ApiFailure) {
                // An unreachable server already came back as an empty page from
                // the repository, so reaching here means a real failure. Show
                // the server's message rather than a generic replacement.
                LoadState.Error(e.message)
            } catch (e: Exception) {
                LoadState.Error("Couldn't load appointments. Try again in a moment.")
            }
            _uiState.value = when (filter) {
                AppointmentFilter.UPCOMING -> _uiState.value.copy(upcoming = result)
                AppointmentFilter.HISTORY -> _uiState.value.copy(history = result)
                AppointmentFilter.ALL -> _uiState.value.copy(
                    upcoming = result,
                    history = result,
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AppointmentsViewModel(AppContainer.appointmentRepository) }
        }
    }
}