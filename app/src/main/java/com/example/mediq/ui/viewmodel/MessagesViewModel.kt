package com.example.mediq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.RetrofitClient
import com.example.mediq.data.model.*
import com.example.mediq.ui.screens.messages.MessagesUiState
import com.example.mediq.ui.screens.messages.SecretaryConversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessagesViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

    private val _uiState = MutableStateFlow<MessagesUiState>(MessagesUiState.Loading)
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = MessagesUiState.Loading
            _error.value = null
            try {
                val threads = api.getConversations()
                val doctors = api.getDoctors().items
                val doctorMap = doctors.associateBy { it.id }

                val conversations = threads.map { thread ->
                    val doctor = doctorMap[thread.doctorId]
                    val displayName = doctor?.fullName ?: "Unknown Doctor"
                    SecretaryConversation(
                        id = thread.id,
                        secretaryName = displayName,
                        initials = displayName.substring(0, 1).uppercase(),
                        doctorName = displayName,
                        preview = thread.subject,
                        date = thread.lastMessageAt,
                        unreadCount = thread.unreadCount,
                    )
                }

                _uiState.value = if (conversations.isEmpty()) {
                    MessagesUiState.Empty
                } else {
                    MessagesUiState.Content(conversations)
                }
            } catch (e: Exception) {
                _uiState.value = MessagesUiState.Error(e.message ?: "Failed to load conversations")
                _error.value = e.message ?: "Failed to load conversations"
            }
        }
    }

    fun sendMessage(conversationId: String, body: String) {
        viewModelScope.launch {
            try {
                api.sendMessage(conversationId, body)
                loadConversations()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send message"
            }
        }
    }
}
