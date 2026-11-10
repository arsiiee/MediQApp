package com.example.mediq.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/** The list itself. */
@Composable
internal fun MessagesList(
    conversations: List<SecretaryConversation>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MediQSpacing.gutter, vertical = MediQSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.md)
    ) {
        items(conversations, key = { it.id }) { conversation ->
            ConversationCard(
                conversation = conversation,
                onClick = { onOpen(conversation.id) }
            )
        }
    }
}

/** Loading / empty / error states for the messages list. */
@Composable
internal fun MessagesEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MediQSpacing.lg)
        ) {
            Text(
                text = "No conversations yet",
                style = MaterialTheme.typography.titleLarge,
                color = MediQTextPrimary
            )

            Text(
                text = "Messages from your clinic secretaries will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun MessagesLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading conversations...",
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextSecondary
        )
    }
}

@Composable
internal fun MessagesErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MediQSpacing.lg)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = MediQSpacing.gutter)
            )

            Button(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = MediQSize.minTouchTarget)
            ) {
                Text(text = "Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}