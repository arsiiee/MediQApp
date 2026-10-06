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
import com.example.mediq.domain.model.RescheduleRequest
import com.example.mediq.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppointmentDetailsUiState(
    val appointment: LoadState<Appointment> = LoadState.Loading,

    /** The message from a refused or undeliverable cancel/reschedule, if any. */
    val actionError: String? = null,
) {
    /**
     * Whether Cancel and Reschedule should be on screen at all.
     *
     * Read off [AppointmentStatus.isActionable] rather than re-decided here, so
     * the rule has exactly one definition. `UNKNOWN` is deliberately not
     * actionable: offering a mutation on a status the app cannot read would mean
     * guessing which endpoint the appointment would accept.
     */
    val canCancelOrReschedule: Boolean
        get() = (appointment as? LoadState.Success)?.data?.status?.isActionable == true
}

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

    /**
     * Cancels this appointment, then reloads it.
     *
     * The reload is not optional. `AppointmentsStore.cancel` frees the slot by
     * deleting the `slot_claims` row and keeps the appointment row for history,
     * so the status only changes on the server — without a re-read the badge
     * would still say "Confirmed" after the patient cancelled, and
     * `canCancelOrReschedule` would keep offering a second cancel that the server
     * would refuse.
     */
    fun cancel() {
        val id = appointmentId
        if (id.isNullOrBlank() || !_uiState.value.canCancelOrReschedule) return

        viewModelScope.launch {
            try {
                appointmentRepository.cancel(id)
                _uiState.value = _uiState.value.copy(actionError = null)
                load()
            } catch (e: BackendNotConnectedException) {
                actionFailed("We couldn't reach the clinic, so this appointment was not cancelled.")
            } catch (e: ApiFailure) {
                actionFailed(e.message)
            } catch (e: Exception) {
                actionFailed("We couldn't cancel this appointment. Please try again.")
            }
        }
    }

    /**
     * Asks the clinic to move this appointment to [slotId].
     *
     * A null [slotId] is refused here rather than sent: the request cannot
     * succeed without one, so submitting it would show the patient a 4xx for a
     * button they were never really offered.
     *
     * The appointment is deliberately *not* reloaded on success. A reschedule
     * request is a request; the status does not change until the clinic answers,
     * and re-reading would show the patient an unchanged appointment that looked
     * like nothing had happened.
     */
    fun requestReschedule(slotId: String?) {
        val id = appointmentId
        if (id.isNullOrBlank() || !_uiState.value.canCancelOrReschedule) return

        if (slotId.isNullOrBlank()) {
            actionFailed("Choose a new time first.")
            return
        }

        viewModelScope.launch {
            try {
                appointmentRepository.requestReschedule(
                    RescheduleRequest(appointmentId = id, requestedSlotId = slotId)
                )
                _uiState.value = _uiState.value.copy(actionError = null)
            } catch (e: BackendNotConnectedException) {
                actionFailed("We couldn't reach the clinic, so the request wasn't sent.")
            } catch (e: ApiFailure) {
                actionFailed(e.message)
            } catch (e: Exception) {
                actionFailed("We couldn't send that request. Please try again.")
            }
        }
    }

    private fun actionFailed(message: String) {
        _uiState.value = _uiState.value.copy(actionError = message)
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
