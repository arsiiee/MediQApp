package com.example.mediq.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.data.repository.BackendNotConnectedException
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.repository.AppointmentRepository
import com.example.mediq.domain.repository.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the home screen draws.
 *
 * Two separate requests rather than one, because a slow slot list should not
 * hold back the next appointment — and because they will realistically fail
 * independently.
 */
data class HomeUiState(
    val nextAppointment: LoadState<Appointment?> = LoadState.Loading,
    val doctorsWithOpenSlots: LoadState<List<Doctor>> = LoadState.Loading,
)

class HomeViewModel(
    private val doctorRepository: DoctorRepository,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadNextAppointment()
        loadDoctorsWithOpenSlots()
    }

    fun refresh() {
        loadNextAppointment()
        loadDoctorsWithOpenSlots()
    }

    private fun loadNextAppointment() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                nextAppointment = try {
                    LoadState.Success(
                        appointmentRepository.getAppointments(AppointmentFilter.UPCOMING)
                            .items
                            .minByOrNull { it.startsAt }
                    )
                } catch (e: BackendNotConnectedException) {
                    LoadState.Success(null)
                } catch (e: ApiFailure) {
                    LoadState.Error(e.message)
                } catch (e: Exception) {
                    LoadState.Error("Couldn't load your appointments. Try again in a moment.")
                }
            )
        }
    }

    private fun loadDoctorsWithOpenSlots() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                doctorsWithOpenSlots = try {
                    LoadState.Success(doctorRepository.getDoctors(DoctorQuery()).items)
                } catch (e: BackendNotConnectedException) {
                    // Expected until a backend exists. Not worth an error banner.
                    LoadState.Success(emptyList())
                } catch (e: ApiFailure) {
                    LoadState.Error(e.message)
                } catch (e: Exception) {
                    LoadState.Error("Couldn't load doctors. Check your connection and try again.")
                }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    doctorRepository = AppContainer.doctorRepository,
                    appointmentRepository = AppContainer.appointmentRepository,
                )
            }
        }
    }
}