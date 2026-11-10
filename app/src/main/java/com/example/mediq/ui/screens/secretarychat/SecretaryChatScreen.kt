package com.example.mediq.ui.screens.secretarychat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.viewmodel.ChatViewModel

/**
 * Secretary chat.
 *
 * Job: resolve a question. Primary action: send.
 *
 * Messages are loaded from the server via [ChatViewModel].
 */
@Composable
fun SecretaryChatScreen(
    navController: NavController,
    conversationId: String,
    secretaryName: String = "Secretary",
    role: String? = null,
    location: String? = null,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val draftText = remember { mutableStateOf("") }

    LaunchedEffect(viewModel, conversationId) {
        viewModel.loadMessages(conversationId)
    }

    SecretaryChatContent(
        state = if (isLoading) {
            ChatUiState.Loading
        } else {
            val errorMessage = error
            if (errorMessage != null) {
                ChatUiState.Error(errorMessage)
            } else {
                ChatUiState.Content(
                    conversationId = conversationId,
                    secretaryName = secretaryName,
                    role = role,
                    location = location,
                    messages = messages,
                )
            }
        },
        actions = ChatActions(
            onBack = { navController.navigate(Screen.Messages.route) { launchSingleTop = true } },
            onSend = { text ->
                if (text.isNotBlank()) {
                    viewModel.sendMessage(conversationId, text)
                    draftText.value = ""
                }
            }
        ),
        draftText = draftText.value,
        onDraftChange = { draftText.value = it },
    )
}

/** State switch. Everything below is navigation-free and previewable. */
@Composable
fun SecretaryChatContent(
    state: ChatUiState,
    actions: ChatActions,
    modifier: Modifier = Modifier,
    draftText: String = "",
    onDraftChange: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        when (state) {
            ChatUiState.Loading -> ChatLoadingState()
            is ChatUiState.Error -> ChatErrorState(message = state.message, onRetry = actions.onBack)
            is ChatUiState.Content -> {
                ChatHeader(
                    secretaryName = state.secretaryName,
                    role = state.role,
                    location = state.location,
                    onBack = actions.onBack
                )
                ChatNotice()

                // Chat list fills the space above the input bar.
                ChatMessageList(
                    messages = state.messages,
                    modifier = Modifier.weight(1f)
                )

                // Input bar pinned to the bottom edge.
                ChatInput(
                    text = draftText,
                    onTextChange = onDraftChange,
                    onSend = { actions.onSend(draftText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.union(WindowInsets.ime)
                        )
                )
            }
        }
    }
}
