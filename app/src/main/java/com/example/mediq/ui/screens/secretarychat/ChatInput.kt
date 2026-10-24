package com.example.mediq.ui.screens.secretarychat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.screens.messages.ChatMessage
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPlaceholder
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.theme.MediQWarning
import com.example.mediq.ui.theme.MediQWarningContainer

/*
 * The pieces that make up the chat screen.
 */

/** The message list. */
@Composable
internal fun ChatMessageList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MediQSpacing.sm, vertical = MediQSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.md)
    ) {
        items(messages) { message ->
            ChatBubble(message = message)
        }
    }
}

/** The send row at the bottom. */
@Composable
internal fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf(text) }

    LaunchedEffect(text) {
        messageText = text
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MediQBackground)
            .padding(horizontal = MediQSpacing.sm, vertical = MediQSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = { onTextChange(it); messageText = it },
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Type your message...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediQTextPlaceholder
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MediQTextPrimary),
            singleLine = true,
            shape = RoundedCornerShape(MediQRadius.pill),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MediQBackground,
                unfocusedContainerColor = MediQBackground,
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = MediQBackground,
                cursorColor = MediQGreen
            )
        )

        Spacer(Modifier.width(MediQSpacing.sm))

        TextButton(
            onClick = onSend,
            enabled = enabled && messageText.isNotBlank(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .size(MediQSize.control)
                .heightIn(min = MediQSize.minTouchTarget)
        ) {
            Icon(
                imageVector = Icons.Outlined.Send,
                contentDescription = "Send",
                tint = MediQGreen,
                modifier = Modifier.size(MediQSize.iconLarge)
            )
        }
    }
}

/** Loading / error states for the chat. */
@Composable
internal fun ChatLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Loading conversation...", style = MaterialTheme.typography.bodyMedium, color = MediQTextSecondary)
    }
}

@Composable
internal fun ChatErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MediQSpacing.lg)
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MediQTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = MediQSpacing.gutter))
            androidx.compose.material3.Button(onClick = onRetry, modifier = Modifier.heightIn(min = MediQSize.minTouchTarget)) {
                Text(text = "Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}