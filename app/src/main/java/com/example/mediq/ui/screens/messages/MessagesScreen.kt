package com.example.mediq.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.viewmodel.MessagesViewModel

/**
 * Messages list.
 *
 * Job: reach the secretary. Primary action: open a conversation.
 *
 * State is loaded from the server via [MessagesViewModel].
 */
@Composable
fun MessagesScreen(
    navController: NavController,
    viewModel: MessagesViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    var readIds by rememberSaveable { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(viewModel) {
        viewModel.loadConversations()
    }

    val resolvedState = when (uiState) {
        is MessagesUiState.Content -> MessagesUiState.Content(
            conversations = uiState.conversations.map { conv ->
                if (conv.id in readIds) conv.copy(unreadCount = 0) else conv
            }
        )
        else -> uiState
    }

    MessagesContent(
        state = resolvedState,
        actions = MessagesActions(
            onOpenChat = { id ->
                readIds = readIds + id
                navController.navigate(Screen.MessageChat.createRoute(id))
            }
        )
    )
}

/** State switch. Everything below is navigation-free and previewable. */
@Composable
fun MessagesContent(
    state: MessagesUiState,
    actions: MessagesActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQBackHeader(title = "Messages", onBack = null)

        var searchQuery by remember { mutableStateOf("") }

        MessagesSearchField(
            query = searchQuery,
            onChange = { searchQuery = it }
        )

        when (state) {
            MessagesUiState.Loading -> MessagesLoadingState()
            is MessagesUiState.Error -> MessagesErrorState(
                message = state.message,
                onRetry = actions.onRetry
            )
            MessagesUiState.Empty -> MessagesEmptyState()
            is MessagesUiState.Content -> MessagesList(
                conversations = state.conversations,
                onOpen = actions.onOpenChat
            )
        }
    }
}