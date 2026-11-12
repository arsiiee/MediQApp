package com.example.mediq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.SupabaseApiService
import com.example.mediq.data.model.AppointmentStatus
import com.example.mediq.ui.session.DateTimeFormatter
import com.example.mediq.ui.session.DisplayName
import com.example.mediq.ui.screens.messages.MessagesUiState
import com.example.mediq.ui.screens.messages.SecretaryConversation
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessagesViewModel : ViewModel() {
    private val api = SupabaseApiService()

    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = MessagesUiState.Loading
            try {
                val threads = api.getConversations()
                if (threads.isEmpty()) {
                    _uiState.value = MessagesUiState.Empty
                    return@launch
                }

                val doctorsById = api.getDoctors().items.associateBy { it.id }
                val doctorsByUserId = api.getDoctors().items.associateBy { it.userId }
                val now = Instant.now()
                val upcomingByDoctor = api.getAppointments()
                    .items
                    .filter { appt ->
                        val isLive = appt.status == AppointmentStatus.BOOKED || appt.status == AppointmentStatus.CONFIRMED
                        val startsAt = runCatching { Instant.parse(appt.startsAt) }.getOrNull()
                        isLive && startsAt != null && startsAt.isAfter(now)
                    }
                    .groupBy { it.doctorId }
                    .mapValues { (_, appts) -> appts.minByOrNull { it.startsAt } }

                val conversations = threads.map { thread ->
                    async {
                        val doctor = doctorsByUserId[thread.doctorId] ?: doctorsById[thread.doctorId]
                        val doctorNameRaw = doctor?.fullName?.takeIf { it.isNotBlank() } ?: "Doctor"
                        val doctorName = if (doctorNameRaw.startsWith("Dr.", ignoreCase = true)) {
                            doctorNameRaw
                        } else {
                            "Dr. $doctorNameRaw"
                        }
                        val clinicName = "${doctorName}'s Office"
                        val lastMessage = runCatching { api.getMessages(thread.id).lastOrNull() }.getOrNull()
                        val preview = lastMessage?.body?.takeIf { it.isNotBlank() } ?: "No messages yet"
                        val lastAt = lastMessage?.sentAt ?: thread.lastMessageAt
                        val upcoming = upcomingByDoctor[thread.doctorId]
                            ?: doctor?.userId?.let { upcomingByDoctor[it] }
                            ?: doctor?.id?.let { upcomingByDoctor[it] }
                        val appointmentDate = upcoming?.startsAt?.let { DateTimeFormatter.formatDate(it) }
                        val appointmentTime = upcoming?.startsAt?.let { DateTimeFormatter.formatTime(it) }

                        SecretaryConversation(
                            id = thread.id,
                            secretaryName = clinicName,
                            initials = DisplayName.initials(doctorNameRaw),
                            doctorName = doctorName,
                            preview = preview,
                            date = lastAt,
                            appointmentDate = appointmentDate,
                            appointmentTime = appointmentTime,
                            unreadCount = thread.unreadCount,
                        )
                    }
                }.awaitAll()

                _uiState.value = if (conversations.isEmpty()) {
                    MessagesUiState.Empty
                } else {
                    MessagesUiState.Content(conversations.sortedByDescending { it.date })
                }
            } catch (e: Exception) {
                _uiState.value = MessagesUiState.Error(e.message ?: "Failed to load messages")
            }
        }
    }
}
