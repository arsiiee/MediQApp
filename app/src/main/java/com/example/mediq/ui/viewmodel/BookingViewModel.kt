package com.example.mediq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.SupabaseApiService
import com.example.mediq.data.model.*
import com.example.mediq.ui.session.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class BookingViewModel : ViewModel() {
    private val api = SupabaseApiService()

    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())
    val doctors: StateFlow<List<Doctor>> = _doctors.asStateFlow()

    private val _specializations = MutableStateFlow<List<Specialty>>(emptyList())
    val specializations: StateFlow<List<Specialty>> = _specializations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bookingResult = MutableStateFlow<Appointment?>(null)
    val bookingResult: StateFlow<Appointment?> = _bookingResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadDoctors(specializationId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = api.getDoctors(specializationId = specializationId)
                _doctors.value = result.items
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load doctors"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSpecializations() {
        viewModelScope.launch {
            try {
                _specializations.value = api.getSpecializations()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load specializations"
            }
        }
    }

    fun bookAppointment(request: BookingRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Frontend guard: prevent the patient from booking another appointment
                // on the same day (including with a different doctor). Server-side RPC enforces this too.
                val patientUserId = api.resolvePatientUserId() ?: request.patientId
                val existing = api.getAppointments(patientId = patientUserId)

                val zone = ZoneId.systemDefault()
                val requestedDate = runCatching {
                    DateTimeFormatter.instant(request.startsAt).atZone(zone).toLocalDate()
                }.getOrNull()

                val conflict = requestedDate != null && existing.items.any { appt ->
                    appt.status != AppointmentStatus.CANCELLED &&
                        appt.status != AppointmentStatus.NO_SHOW &&
                        runCatching {
                            DateTimeFormatter.instant(appt.startsAt).atZone(zone).toLocalDate() == requestedDate
                        }.getOrDefault(false)
                }
                if (conflict) {
                    _error.value = "You already have an appointment on this day."
                    return@launch
                }

                val appointment = api.createAppointment(request)
                _bookingResult.value = appointment
            } catch (e: Exception) {
                _error.value = e.message ?: "Booking failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun overlaps(existingStartsAt: String, existingDuration: Int, newStart: Instant, newEnd: Instant): Boolean {
        val existingStart = DateTimeFormatter.instant(existingStartsAt)
        val existingEnd = existingStart.plus(Duration.ofMinutes(existingDuration.toLong()))
        // Two time ranges overlap if each starts before the other ends
        return newStart.isBefore(existingEnd) && existingStart.isBefore(newEnd)
    }

    fun clearBookingResult() {
        _bookingResult.value = null
    }
}