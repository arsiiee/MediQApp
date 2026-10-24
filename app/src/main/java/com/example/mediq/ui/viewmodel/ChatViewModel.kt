package com.example.mediq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.RetrofitClient
import com.example.mediq.data.model.Message
import com.example.mediq.data.model.MessageAuthorRole
import com.example.mediq.ui.screens.messages.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val api = RetrofitClient.apiService

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
                val serverMessages = api.getMessages(conversationId)
                _messages.value = serverMessages.map { m ->
                    ChatMessage(
                        text = m.body,
                        sentByPatient = m.authorRole == MessageAuthorRole.PATIENT,
                        time = m.sentAt,
                    )
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load messages"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(conversationId: String, body: String) {
        viewModelScope.launch {
            try {
                api.sendMessage(conversationId, body)
                loadMessages(conversationId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send message"
            }
        }
    }
}
