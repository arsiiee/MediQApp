package com.example.mediq.ui.feature.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentStatus
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.RescheduleRequest
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.repository.AppointmentRepository
import com.example.mediq.domain.repository.DoctorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class AppointmentDetailsUiState(
    val appointment: LoadState<Appointment> = LoadState.Loading,

    /** The message from a refused or undeliverable cancel/reschedule, if any. */
    val actionError: String? = null,

    // ── Reschedule picker ──────────────────────────────────────────────
    // Fetched only once the patient opens it. Two requests on every visit to the
    // screen would be spent on a picker most visits never open.

    val isReschedulePickerOpen: Boolean = false,
    val availableDates: LoadState<List<AvailableDate>> = LoadState.Loading,
    val selectedDate: LocalDate? = null,
    val slots: LoadState<List<TimeSlot>> = LoadState.Loading,

    /** Bookable slots only — the ones the picker is allowed to offer. */
    val selectedSlot: TimeSlot? = null,
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

    /** The Send button stays disabled until a real slot is chosen. */
    val canSendRescheduleRequest: Boolean
        get() = isReschedulePickerOpen && selectedSlot != null
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
    private val doctorRepository: DoctorRepository,
    private val appointmentId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentDetailsUiState())
    val uiState: StateFlow<AppointmentDetailsUiState> = _uiState.asStateFlow()

    /**
     * The in-flight slots fetch, cancelled when the patient picks another date.
     *
     * Without this, a slow response for Tuesday lands after Wednesday was tapped
     * and replaces Wednesday's list with Tuesday's — while the patient is looking
     * at Wednesday. `DoctorDetailsViewModel` carries the same guard for the same
     * reason.
     */
    private var slotsJob: Job? = null

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

    // --- The reschedule picker ----------------------------------------------
    //
    // Availability comes from `DoctorRepository`, not `AppointmentRepository`:
    // `/doctors/{id}/availability` and `/doctors/{id}/slots` belong to the doctor
    // aggregate, and the domain split keeps them there.

    /**
     * Opens the picker and loads this doctor's available dates.
     *
     * Refused for an appointment that is not actionable, and not merely hidden:
     * opening it would fetch availability the patient cannot use, and a
     * reschedule on a cancelled appointment is a request the server rejects.
     */
    fun openReschedulePicker() {
        if (!_uiState.value.canCancelOrReschedule) return

        val doctorId = currentDoctorId() ?: return

        _uiState.value = _uiState.value.copy(
            isReschedulePickerOpen = true,
            availableDates = LoadState.Loading,
            selectedDate = null,
            slots = LoadState.Loading,
            selectedSlot = null,
            actionError = null,
        )
        viewModelScope.launch {
            val result = try {
                LoadState.Success(
                    doctorRepository.getAvailableDates(doctorId, YearMonth.now().atDay(1))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendNotConnectedException) {
                // A list read: "nothing to show" and "no backend" look the
                // same, and an error here would put a red sentence above an
                // appointment the patient can still cancel.
                LoadState.Success(emptyList())
            } catch (e: ApiFailure) {
                LoadState.Error(e.message)
            } catch (e: Exception) {
                LoadState.Error("Couldn't load available dates.")
            }
            _uiState.update { it.copy(availableDates = result) }
        }
    }

    fun closeReschedulePicker() {
        slotsJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isReschedulePickerOpen = false,
            selectedDate = null,
            slots = LoadState.Loading,
            // A slot chosen in a previous visit to the picker must not be
            // resubmitted by the next one.
            selectedSlot = null,
        )
    }

    /**
     * Selects a date and loads its slots, discarding any earlier selection.
     *
     * Discarding is the point: otherwise the patient picks 9:00 on Tuesday, taps
     * Wednesday by mistake, and submits a Tuesday slot while reading Wednesday's
     * list.
     */
    fun onDateSelected(date: LocalDate) {
        if (!_uiState.value.isReschedulePickerOpen) return

        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            selectedSlot = null,
            slots = LoadState.Loading,
        )
        slotsJob?.cancel()
        slotsJob = viewModelScope.launch { loadSlots(date) }
    }

    fun onSlotSelected(slot: TimeSlot) {
        if (!_uiState.value.isReschedulePickerOpen) return

        // Only bookable slots are ever handed to this method — the filter lives
        // here so a test can pin it rather than trusting the composable.
        if (!slot.isBookable) return

        _uiState.value = _uiState.value.copy(selectedSlot = slot)
    }

    private suspend fun loadSlots(date: LocalDate) {
        val doctorId = currentDoctorId() ?: return

        val result = try {
            LoadState.Success(
                doctorRepository.getSlots(doctorId, date).filter { it.isBookable }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackendNotConnectedException) {
            LoadState.Success(emptyList())
        } catch (e: ApiFailure) {
            LoadState.Error(e.message)
        } catch (e: Exception) {
            LoadState.Error("Couldn't load slots for that date.")
        }
        _uiState.update { it.copy(slots = result) }
    }

    /** The doctor this appointment is with — availability is per doctor. */
    private fun currentDoctorId(): String? =
        (_uiState.value.appointment as? LoadState.Success)?.data?.doctor?.id

    /**
     * Asks the clinic to move this appointment to the slot the patient chose.
     *
     * Reads the slot from state rather than taking it as an argument, so the
     * screen cannot submit a slot the picker never showed them.
     *
     * The appointment is deliberately *not* reloaded on success. A reschedule is
     * a request: nothing changes until the clinic answers, and re-reading would
     * show the patient an unchanged appointment that looked like nothing had
     * happened.
     */
    fun requestReschedule() {
        val id = appointmentId
        if (id.isNullOrBlank() || !_uiState.value.canCancelOrReschedule) return

        val slotId = _uiState.value.selectedSlot?.id
        if (slotId == null) {
            actionFailed("Choose a new time first.")
            return
        }

        viewModelScope.launch {
            try {
                appointmentRepository.requestReschedule(
                    RescheduleRequest(appointmentId = id, requestedSlotId = slotId)
                )
                // Cleared here rather than inside `closeReschedulePicker`, which
                // the patient can also call — and an error that has just been
                // resolved should not outlive the fix.
                _uiState.value = _uiState.value.copy(actionError = null)
                closeReschedulePicker()
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
                    doctorRepository = AppContainer.doctorRepository,
                    appointmentId = appointmentId,
                )
            }
        }
    }
}
