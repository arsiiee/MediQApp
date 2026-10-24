package com.example.mediq.ui.screens.secretarychat

import com.example.mediq.ui.screens.messages.MockConversations
import com.example.mediq.ui.screens.messages.mockMessagesFor

/*
 * Named states for the chat.
 */
object SecretaryChatSamples {

    val loading: ChatUiState = ChatUiState.Loading

    val error: ChatUiState = ChatUiState.Error(
        "We could not load this conversation. Check your connection and try again."
    )

    val maria: ChatUiState = ChatUiState.Content(
        conversationId = "maria_secretary",
        secretaryName = "Ms. Liza Fuentes",
        messages = mockMessagesFor("maria_secretary")
    )

    val kathleen: ChatUiState = ChatUiState.Content(
        conversationId = "kathleen_secretary",
        secretaryName = "Ms. Karen Lopez",
        messages = mockMessagesFor("kathleen_secretary")
    )
}