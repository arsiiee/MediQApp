package com.example.mediq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.SupabaseApiService
import com.example.mediq.data.model.Appointment as ApiAppointment
import com.example.mediq.data.model.AppointmentStatus as ApiAppointmentStatus
import com.example.mediq.data.model.Doctor
import com.example.mediq.data.model.MessageThread
import com.example.mediq.ui.session.DateTimeFormatter
import com.example.mediq.ui.session.DisplayName
import com.example.mediq.ui.screens.appointments.AppointmentBooking
import com.example.mediq.ui.screens.appointments.AppointmentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeUnit

class AppointmentsViewModel : ViewModel() {
    private val api = SupabaseApiService()

    private val _appointments = MutableStateFlow<List<AppointmentBooking>>(emptyList())
    val appointments: StateFlow<List<AppointmentBooking>> = _appointments.asStateFlow()

    private val _appointmentDetail = MutableStateFlow<AppointmentBooking?>(null)
    val appointmentDetail: StateFlow<AppointmentBooking?> = _appointmentDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Doctors cache to avoid redundant Supabase queries on every screen open
    private var _cachedDoctors: List<Doctor>? = null
    private var _doctorsCacheTime: Long = 0
    private val DOCTORS_CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5)

    private suspend fun loadDoctors(): List<Doctor> {
        val now = System.currentTimeMillis()
        if (_cachedDoctors == null || now - _doctorsCacheTime > DOCTORS_CACHE_TTL_MS) {
            _cachedDoctors = api.getDoctors().items
            _doctorsCacheTime = now
        }
        return _cachedDoctors ?: emptyList()
    }

    /** Loads the patient's real bookings from the API, never sample data. */
    fun loadAppointments() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Resolve users.id from auth_user_id so appointments are filtered
                // by the correct patient_id, regardless of RLS policy configuration.
                val patientUserId = api.resolvePatientUserId()
                android.util.Log.d("MediQBookingDiag", "AppointmentsViewModel.loadAppointments patientUserId=$patientUserId")
                val result = api.getAppointments(patientId = patientUserId)
                val doctors = loadDoctors()
                // getConversations can fail if the profiles/users lookup is blocked by RLS;
                // degrade gracefully to an empty list rather than crashing the screen.
                val conversations = try {
                    api.getConversations()
                } catch (e: Exception) {
                    emptyList()
                }
                val now = Clock.systemDefaultZone().instant()
                val pairs = result.items.map { apiItem -> apiItem to toBooking(apiItem, doctors, conversations) }
                val upcoming = pairs
                    .filter { (apiItem, booking) ->
                        (booking.status == AppointmentStatus.Confirmed || booking.status == AppointmentStatus.AwaitingConfirmation) &&
                            runCatching { !DateTimeFormatter.instant(apiItem.startsAt).isBefore(now) }.getOrDefault(false)
                    }
                    .map { it.second }
                val upcomingIds = upcoming.map { it.id }.toSet()
                // Everything not currently upcoming (past, cancelled, completed, no-show, or past booked/confirmed)
                // belongs in history so appointments never disappear from the page.
                val history = pairs.map { it.second }.filter { it.id !in upcomingIds }
                android.util.Log.d("MediQBookingDiag", "AppointmentsViewModel mapped all=${pairs.size} upcoming=${upcoming.size} history=${history.size} now=$now")
                _appointments.value = upcoming + history
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load appointments"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Loads one real booking by id, used by the detail screen. */
    fun loadAppointment(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val appt = api.getAppointment(id)
                val doctors = loadDoctors()
                val conversations = api.getConversations()
                _appointmentDetail.value = toBooking(appt, doctors, conversations)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load appointment"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun cancelAppointment(id: String, reason: String): Boolean {
        _error.value = null
        return try {
            val patientUserId = api.resolvePatientUserId()
                ?: throw RuntimeException("Could not resolve patient id")
            api.cancelAppointment(id, patientUserId, reason)
            loadAppointments()
            true
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to cancel appointment"
            false
        }
    }

    private fun toBooking(
        appt: ApiAppointment,
        doctors: List<Doctor>,
        conversations: List<MessageThread>
    ): AppointmentBooking {
        val doctor = doctors.find { it.id == appt.doctorId || it.userId == appt.doctorId }
        val conversation = conversations.firstOrNull {
            it.patientId == appt.patientId && it.doctorId == appt.doctorId
        }
        return AppointmentBooking(
            id = appt.id,
            doctor = doctor?.fullName ?: "Unknown doctor",
            initials = DisplayName.initials(doctor?.fullName ?: "Unknown"),
            specialty = doctor?.specialty ?: "",
            dateShort = DateTimeFormatter.formatDate(appt.startsAt),
            dateLong = DateTimeFormatter.formatDateLong(appt.startsAt),
            time = DateTimeFormatter.formatTime(appt.startsAt),
            location = DateTimeFormatter.formatLocation(doctor?.location),
            reason = appt.reason,
            status = toUiStatus(appt.status),
            conversationId = conversation?.id ?: ""
        )
    }

    private fun toUiStatus(status: ApiAppointmentStatus): AppointmentStatus =
        when (status) {
            ApiAppointmentStatus.CONFIRMED -> AppointmentStatus.Confirmed
            ApiAppointmentStatus.BOOKED -> AppointmentStatus.AwaitingConfirmation
            ApiAppointmentStatus.COMPLETED -> AppointmentStatus.Completed
            ApiAppointmentStatus.CANCELLED -> AppointmentStatus.Cancelled
            ApiAppointmentStatus.NO_SHOW -> AppointmentStatus.Completed
        }
}
