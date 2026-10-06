package com.example.mediq.ui.feature.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.data.repository.BackendNotConnectedException
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.repository.DoctorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DoctorDetailsUiState(
    val doctor: LoadState<Doctor> = LoadState.Loading,
    val availableDates: LoadState<List<AvailableDate>> = LoadState.Loading,
    val selectedDate: LocalDate? = null,
    val slots: LoadState<List<TimeSlot>> = LoadState.Loading,
    val selectedSlot: TimeSlot? = null,
) {
    val canContinue: Boolean get() = selectedSlot != null
}

/**
 * Loads the doctor's profile and their open slots.
 *
 * Selecting a date fetches that date's slots. The previous fetch is cancelled
 * first, so a slow request for an earlier date cannot overwrite the slots for
 * the date the user is actually looking at.
 */
class DoctorDetailsViewModel(
    private val doctorRepository: DoctorRepository,
    private val doctorId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorDetailsUiState())
    val uiState: StateFlow<DoctorDetailsUiState> = _uiState.asStateFlow()

    private var slotsJob: Job? = null

    init {
        loadDoctor()
        loadAvailableDates()
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            selectedSlot = null,
            slots = LoadState.Loading,
        )
        slotsJob?.cancel()
        slotsJob = viewModelScope.launch { loadSlots(date) }
    }

    fun onSlotSelected(slot: TimeSlot) {
        _uiState.value = _uiState.value.copy(selectedSlot = slot)
    }

    private fun loadDoctor() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                doctor = try {
                    if (doctorId == null) {
                        LoadState.Error("No doctor was selected.")
                    } else {
                        LoadState.Success(doctorRepository.getDoctor(doctorId))
                    }
                } catch (e: BackendNotConnectedException) {
                    LoadState.Error("This doctor's profile isn't available yet.")
                } catch (e: ApiFailure) {
                    LoadState.Error(e.message)
                } catch (e: Exception) {
                    LoadState.Error("Couldn't load this doctor. Try again in a moment.")
                }
            )
        }
    }

    private fun loadAvailableDates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                availableDates = try {
                    val dates = if (doctorId == null) {
                        emptyList()
                    } else {
                        doctorRepository.getAvailableDates(doctorId, YearMonth.now().atDay(1))
                    }
                    LoadState.Success(dates)
                } catch (e: BackendNotConnectedException) {
                    LoadState.Success(emptyList())
                } catch (e: ApiFailure) {
                    LoadState.Error(e.message)
                } catch (e: Exception) {
                    LoadState.Error("Couldn't load available dates.")
                }
            )
        }
    }

    private suspend fun loadSlots(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            slots = try {
                val loaded = if (doctorId == null) emptyList() else doctorRepository.getSlots(doctorId, date)
                LoadState.Success(loaded)
            } catch (e: BackendNotConnectedException) {
                LoadState.Success(emptyList())
            } catch (e: ApiFailure) {
                LoadState.Error(e.message)
            } catch (e: Exception) {
                LoadState.Error("Couldn't load slots for that date.")
            }
        )
    }

    companion object {
        fun factory(doctorId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { DoctorDetailsViewModel(AppContainer.doctorRepository, doctorId) }
        }
    }
}