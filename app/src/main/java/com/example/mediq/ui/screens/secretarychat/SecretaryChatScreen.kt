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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.session.DateTimeFormatter
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.viewmodel.ChatViewModel
import java.time.Instant

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
    doctorName: String? = null,
    role: String? = null,
    location: String? = null,
    appointmentDate: String = "",
    appointmentTime: String = "",
    isAppointmentCancelled: Boolean = false,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val draftText = remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateOf(emptySet<String>()) }

    val appointmentDateTime = if (appointmentDate.isNotEmpty() && appointmentTime.isNotEmpty()) {
        try {
            DateTimeFormatter.instant("$appointmentDate $appointmentTime")
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }

    val isAppointmentPast = appointmentDateTime != null && appointmentDateTime.isBefore(Instant.now())

    LaunchedEffect(viewModel, conversationId) {
        viewModel.loadMessages(conversationId)
    }

    val canDelete = messages.any { it.sentByPatient }

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
                    doctorName = doctorName,
                    appointmentDate = appointmentDate,
                    appointmentTime = appointmentTime,
                    messages = messages,
                )
            }
        },
        actions = ChatActions(
            onBack = { navController.navigate(Screen.Messages.route) { launchSingleTop = true } },
            onSend = { text ->
                if (isAppointmentPast) {
                    // Block sending when appointment is in the past
                } else if (text.isNotBlank()) {
                    viewModel.sendMessage(conversationId, text)
                    draftText.value = ""
                }
            },
            onDeleteConversation = {
                // Remove from patient's view only; do NOT delete from the
                // database so the secretary still sees the conversation.
                navController.navigate(Screen.Messages.route) { launchSingleTop = true }
            },
        ),
        draftText = draftText.value,
        onDraftChange = { draftText.value = it },
        selectedIds = selectedIds.value,
        onToggleSelection = { id ->
            selectedIds.value = if (selectedIds.value.contains(id)) {
                selectedIds.value - id
            } else {
                selectedIds.value + id
            }
        },
        isAppointmentCancelled = isAppointmentCancelled,
        isAppointmentPast = isAppointmentPast,
        appointmentTime = appointmentTime,
    )
}

/** State switch. Everything below is navigation-free and previewable. */
@Composable
fun SecretaryChatContent(
    state: ChatUiState,
    actions: ChatActions,
    modifier: Modifier = Modifier,
    draftText: String = "",
    onDraftChange: (String) -> Unit = {},
    selectedIds: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    isAppointmentCancelled: Boolean = false,
    isAppointmentPast: Boolean = false,
    appointmentTime: String = "",
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
                    doctorName = state.doctorName,
                    appointmentDate = state.appointmentDate,
                    appointmentTime = state.appointmentTime,
                    onBack = actions.onBack,
                    onDelete = if (!isAppointmentCancelled) { actions.onDeleteConversation } else null,
                )

                if (isAppointmentCancelled) {
                    ChatCancelledNotice()
                } else if (isAppointmentPast) {
                    ChatPastAppointmentNotice()
                } else {
                    ChatNotice()
                }

                // Chat list fills the space above the input bar.
                ChatMessageList(
                    messages = state.messages,
                    selectedIds = selectedIds,
                    onToggleSelection = onToggleSelection,
                    modifier = Modifier.weight(1f),
                    appointmentTime = state.appointmentTime
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
                        ),
                    enabled = !isAppointmentCancelled && !isAppointmentPast,
                )
            }
        }
    }
}
