package com.example.mediq.ui.screens.secretarychat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing

/** The green header with back and the secretary's name. */
@Composable
internal fun ChatHeader(
    secretaryName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MediQSize.headerBar)
            .background(MediQGreen)
            .padding(horizontal = MediQSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to messages",
                tint = MediQOnBrand,
                modifier = Modifier.size(MediQSize.minTouchTarget)
            )
        }

        Column {
            Text(
                text = secretaryName,
                style = MaterialTheme.typography.titleMedium,
                color = MediQOnBrand
            )
        }
    }
}

/** The notice pinned under the header. */
@Composable
internal fun ChatNotice(modifier: Modifier = Modifier) {
    Text(
        text = "For appointment concerns only. This channel is not for medical advice or emergencies.",
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MediQSpacing.xl,
                vertical = MediQSpacing.sm
            ),
        style = MaterialTheme.typography.bodySmall,
        color = com.example.mediq.ui.theme.MediQTextSecondary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}