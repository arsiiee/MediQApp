package com.example.mediq.ui.feature.doctors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.CLINIC_ZONE
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.SlotStatus
import com.example.mediq.domain.model.TimeSlot
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

/**
 * [today] is the date at the clinic, not on the phone, and [month] is the month
 * the picker is showing.
 *
 * Both are in the state rather than read from the clock inside a getter, so the
 * state stays a pure function of itself. A `currentMonth` getter that called
 * `LocalDate.now()` would answer differently a second later, would make
 * `canGoToPreviousMonth` untestable without moving the real clock, and is the
 * same shape as the `BookingUiState.canSubmit` bug `AGENTS.md` calls out.
 */
data class DoctorDetailsUiState(
    val doctor: LoadState<Doctor> = LoadState.Loading,
    val today: LocalDate = LocalDate.now(CLINIC_ZONE),
    val month: YearMonth = YearMonth.from(today),
    val availableDates: LoadState<List<AvailableDate>> = LoadState.Loading,
    val selectedDate: LocalDate? = null,
    val slots: LoadState<List<TimeSlot>> = LoadState.Loading,
    val selectedSlot: TimeSlot? = null,
) {
    val canContinue: Boolean get() = selectedSlot != null

    /**
     * What the picker is allowed to draw: everything bookable, plus the slots
     * another patient has already claimed.
     *
     * Figure 4 draws the claimed ones grey under the legend *"Grey = already
     * claimed by another patient"*, and `MediQSlotChip` has a `Claimed` state
     * built for exactly that - but the screen filtered to `isBookable`, so the
     * legend had nothing to describe. A claimed chip is still not clickable, so
     * what the booking *can do* is unchanged.
     *
     * The split is on the legend's truth, not on bookability: a `BLOCKED` slot
     * is the clinic's own leave rather than another patient's appointment, and
     * an `UNKNOWN` one is a status this build cannot read - drawing either under
     * that sentence would say something the app does not know.
     *
     * A view over [slots], never a rewrite of it: [slots] stays exactly what the
     * server returned.
     */
    val visibleSlots: LoadState<List<TimeSlot>>
        get() = when (slots) {
            is LoadState.Success -> LoadState.Success(
                slots.data.filter {
                    it.status == SlotStatus.AVAILABLE || it.status == SlotStatus.RESERVED
                },
            )

            else -> slots
        }

    /** The clinic's own month, which is the earliest month worth looking at. */
    val currentMonth: YearMonth get() = YearMonth.from(today)

    /**
     * A month already gone holds only dates that cannot be booked, so the stepper
     * stops here.
     *
     * There is deliberately no matching ceiling. A month the doctor never works
     * comes back from the server as an empty list, which is a truthful answer; an
     * invented "no further than N months" rule would be a guess that silently
     * hides real availability.
     */
    val canGoToPreviousMonth: Boolean get() = month.isAfter(currentMonth)
}

/**
 * Loads the doctor's profile and their open slots.
 *
 * Selecting a date fetches that date's slots. The previous fetch is cancelled
 * first, so a slow request for an earlier date cannot overwrite the slots for
 * the date the user is actually looking at.
 *
 * The visible month is steppable. `getAvailableDates` was only ever asked about
 * `YearMonth.now()`, so a doctor's next month could not be seen and, once the
 * month rolled over, there was no way back — the gap `SPEC-ui-redesign` item 6
 * records. Stepping re-reads availability for the month now on screen.
 */
class DoctorDetailsViewModel(
    private val doctorRepository: DoctorRepository,
    private val doctorId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorDetailsUiState())
    val uiState: StateFlow<DoctorDetailsUiState> = _uiState.asStateFlow()

    private var slotsJob: Job? = null
    private var datesJob: Job? = null

    init {
        loadDoctor()
        datesJob = viewModelScope.launch { loadAvailableDates(_uiState.value.month) }
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

    /** One month back, or nothing at all when the picker is already on this one. */
    fun onPreviousMonth() {
        val state = _uiState.value
        if (!state.canGoToPreviousMonth) return
        showMonth(state.month.minusMonths(1))
    }

    fun onNextMonth() = showMonth(_uiState.value.month.plusMonths(1))

    private fun showMonth(month: YearMonth) {
        _uiState.update {
            it.copy(
                month = month,
                // The date and the slot that were chosen belong to the month being
                // left, so they are not in the list about to arrive. Keeping them
                // would leave Continue enabled and booking the wrong day.
                selectedDate = null,
                selectedSlot = null,
                slots = LoadState.Loading,
                availableDates = LoadState.Loading,
            )
        }
        datesJob?.cancel()
        datesJob = viewModelScope.launch { loadAvailableDates(month) }
    }

    private fun loadDoctor() {
        viewModelScope.launch {
            // The suspend (getDoctor) must finish BEFORE this reads the latest
            // value — `_uiState.value.copy(doctor = try {...})` evaluates the
            // receiver first, so a concurrent dates update would be clobbered.
            val result = try {
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
            _uiState.update { it.copy(doctor = result) }
        }
    }

    private suspend fun loadAvailableDates(month: YearMonth) {
        // Same shape as loadSlots: compute first, assign atomically. The previous
        // form evaluated `_uiState.value` before the suspend, so a slow response
        // could resurrect the month the user had already stepped away from.
        val result = try {
            val dates = if (doctorId == null) {
                emptyList()
            } else {
                doctorRepository.getAvailableDates(doctorId, month.atDay(1))
            }
            LoadState.Success(dates)
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackendNotConnectedException) {
            LoadState.Success(emptyList())
        } catch (e: ApiFailure) {
            LoadState.Error(e.message)
        } catch (e: Exception) {
            LoadState.Error("Couldn't load available dates.")
        }
        _uiState.update { it.copy(availableDates = result) }
    }

    private suspend fun loadSlots(date: LocalDate) {
        // Compute first, assign atomically. The original form evaluated
        // `_uiState.value` before the suspend, so a later slots load could
        // overwrite an earlier one's Loading placeholder, and inherits the same
        // stale-copy clobbering as loadDoctor.
        val result = try {
            val loaded = if (doctorId == null) emptyList() else doctorRepository.getSlots(doctorId, date)
            LoadState.Success(loaded)
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

    companion object {
        fun factory(doctorId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { DoctorDetailsViewModel(AppContainer.doctorRepository, doctorId) }
        }
    }
}
