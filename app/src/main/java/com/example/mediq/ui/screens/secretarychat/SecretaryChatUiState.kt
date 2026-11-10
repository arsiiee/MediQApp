package com.example.mediq.ui.screens.secretarychat

import com.example.mediq.ui.screens.messages.ChatMessage

/*
 * What the chat screen can be doing.
 */
sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Error(val message: String) : ChatUiState
    data class Content(
        val conversationId: String,
        val secretaryName: String,
        val role: String? = null,
        val location: String? = null,
        val messages: List<ChatMessage>
    ) : ChatUiState
}

data class ChatActions(
    val onBack: () -> Unit = {},
    val onSend: (String) -> Unit = {}
)