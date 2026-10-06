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
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppointmentDetailsUiState(
    val appointment: LoadState<Appointment> = LoadState.Loading,
)

/**
 * Loads the one appointment named by [appointmentId].
 *
 * The id arrives from the route, so the factory is built from it rather than
 * being a `val` — the same shape `DoctorDetailsViewModel.factory(doctorId)`
 * already uses.
 */
class AppointmentDetailsViewModel(
    private val appointmentRepository: AppointmentRepository,
    private val appointmentId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentDetailsUiState())
    val uiState: StateFlow<AppointmentDetailsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        // No id means the route was malformed. There is nothing to fetch, so
        // nothing is fetched — and the state says why rather than spinning.
        val id = appointmentId
        if (id.isNullOrBlank()) {
            _uiState.value = AppointmentDetailsUiState(
                appointment = LoadState.Error("No appointment was selected.")
            )
            return
        }

        _uiState.value = AppointmentDetailsUiState(appointment = LoadState.Loading)
        viewModelScope.launch {
            _uiState.value = AppointmentDetailsUiState(
                appointment = try {
                    LoadState.Success(appointmentRepository.getAppointment(id))
                } catch (e: BackendNotConnectedException) {
                    // Error, not `Success(emptyList())` as on the list screens.
                    // That mapping is for reads where "no results" and "no
                    // backend" look the same; here a success carrying nothing
                    // would tell a patient with a real appointment that it does
                    // not exist.
                    LoadState.Error("This appointment isn't available right now.")
                } catch (e: ApiFailure) {
                    // Includes the server's 404 "That appointment was not found."
                    LoadState.Error(e.message)
                } catch (e: Exception) {
                    LoadState.Error("Couldn't load this appointment. Try again in a moment.")
                }
            )
        }
    }

    companion object {
        fun factory(appointmentId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppointmentDetailsViewModel(
                    appointmentRepository = AppContainer.appointmentRepository,
                    appointmentId = appointmentId,
                )
            }
        }
    }
}
