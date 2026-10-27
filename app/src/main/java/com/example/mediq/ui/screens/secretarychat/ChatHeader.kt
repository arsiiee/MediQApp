package com.example.mediq.ui.screens.secretarychat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextSecondary

/** The light header with back, secretary name, and role/location subtitle. */
@Composable
internal fun ChatHeader(
    secretaryName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    role: String? = null,
    location: String? = null
) {
    val subtitle = buildString {
        if (role != null) append(role)
        if (location != null) {
            if (role != null) append(" · ")
            append(location)
        }
    }

    MediQPageHeader(
        title = secretaryName,
        subtitle = subtitle.takeIf { it.isNotEmpty() },
        onBack = onBack,
        modifier = modifier
    )
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