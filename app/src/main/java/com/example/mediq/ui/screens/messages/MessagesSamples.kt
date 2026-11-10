package com.example.mediq.ui.screens.messages

/*
 * Named states for the messages list and the chat.
 */
object MessagesSamples {

    // Messages list

    val loading: MessagesUiState = MessagesUiState.Loading

    val error: MessagesUiState = MessagesUiState.Error(
        "We could not load your conversations. Check your connection and try again."
    )

    val empty: MessagesUiState = MessagesUiState.Empty

    val content: MessagesUiState = MessagesUiState.Content(MockConversations)

    // Chat

    val mariaId = "maria_secretary"
    val kathleenId = "kathleen_secretary"
    val joelId = "joel_secretary"
}