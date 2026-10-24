package com.example.mediq.ui.screens.secretarychat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.screens.messages.ChatMessage
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/** One message bubble. */
@Composable
internal fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.sentByPatient) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = MediQSize.chatBubbleMax),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                topStart = MediQRadius.md,
                topEnd = MediQRadius.md,
                bottomStart = if (message.sentByPatient) MediQRadius.md else 3.dp,
                bottomEnd = if (message.sentByPatient) 3.dp else MediQRadius.md
            ),
            color = if (message.sentByPatient) MediQGreen else MediQBackground
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(MediQSpacing.md),
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.sentByPatient) MediQOnBrand else MediQTextPrimary
            )
        }

        Spacer(Modifier.height(MediQSpacing.xxs))

        Text(
            text = message.time,
            style = MaterialTheme.typography.labelSmall,
            color = MediQTextSecondary
        )
    }
}