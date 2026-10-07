package com.example.mediq.ui.feature.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookingUiState(
    val reasonForVisit: String = "",
    val confirmedByPatient: Boolean = false,
    val isSubmitting: Boolean = false,
    val booked: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = confirmedByPatient && !isSubmitting && BookingSelection.selection != null
}

/**
 * Sends the booking. The slot comes from [BookingSelection] rather than from
 * the arguments, so the request can't be built without a real selection.
 */
class BookingViewModel(
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    fun onReasonChanged(reason: String) {
        _uiState.value = _uiState.value.copy(reasonForVisit = reason, error = null)
    }

    fun onConfirmedChanged(confirmed: Boolean) {
        _uiState.value = _uiState.value.copy(confirmedByPatient = confirmed, error = null)
    }

    fun submit() {
        val selection = BookingSelection.selection ?: run {
            _uiState.value = _uiState.value.copy(error = "Choose a time slot first.")
            return
        }
        if (_uiState.value.isSubmitting) return

        _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            try {
                appointmentRepository.book(
                    BookingRequest(
                        slotId = selection.slotId,
                        reasonForVisit = _uiState.value.reasonForVisit.takeIf { it.isNotBlank() },
                        confirmedByPatient = true,
                    )
                )
                _uiState.value = _uiState.value.copy(isSubmitting = false, booked = true)
                // Clear the global selection so a stale pick can't be re-submitted
                // if the user navigates back and enters the booking flow again.
                BookingSelection.clear()
            } catch (e: BackendNotConnectedException) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Bookings aren't available yet. No appointment was created.",
                )
            } catch (e: ApiFailure) {
                // The 409 that matters here is `slot_taken`: "That time was
                // just taken. Please pick another." A generic string told the
                // patient to retry a booking that cannot succeed, because the
                // slot is gone. Nothing was booked, which the message implies
                // only because the server says so.
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Couldn't complete the booking. Please try again.",
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { BookingViewModel(AppContainer.appointmentRepository) }
        }
    }
}