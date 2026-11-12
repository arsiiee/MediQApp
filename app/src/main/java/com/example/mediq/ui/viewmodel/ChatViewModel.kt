package com.example.mediq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.SupabaseApiService
import com.example.mediq.data.model.Message
import com.example.mediq.data.model.MessageAuthorRole
import com.example.mediq.ui.screens.messages.ChatMessage
import com.example.mediq.ui.screens.secretarychat.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val api = SupabaseApiService()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val apiMessages = api.getMessages(conversationId)
            _messages.value = apiMessages.map { msg ->
                ChatMessage(
                    id = msg.id,
                    text = msg.body,
                    sentByPatient = msg.authorRole == com.example.mediq.data.model.MessageAuthorRole.PATIENT,
                    time = msg.sentAt,
                    appointmentId = msg.appointmentId,
                )
            }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load messages"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(conversationId: String, text: String, appointmentId: String? = null) {
        viewModelScope.launch {
            _error.value = null
            try {
                val sent = api.sendMessage(conversationId, text, appointmentId)
                _messages.value = _messages.value + listOf(
                    ChatMessage(
                        id = sent.id,
                        text = sent.body,
                        sentByPatient = sent.authorRole == MessageAuthorRole.PATIENT,
                        time = sent.sentAt.toString(),
                        appointmentId = sent.appointmentId,
                    )
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send message"
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                api.deleteMessage(messageId)
                _messages.value = _messages.value.filter { it.id != messageId }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete message"
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                api.deleteConversation(conversationId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete conversation"
            }
        }
    }
}

