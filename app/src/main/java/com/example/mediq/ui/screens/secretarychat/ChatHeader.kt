package com.example.mediq.ui.screens.secretarychat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQAvatarBrandTile
import com.example.mediq.ui.theme.MediQDarkGreen
import com.example.mediq.ui.theme.MediQHeaderControl
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextSecondary
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete

/** The chat header with secretary name, doctor name, and appointment time. */
@Composable
internal fun ChatHeader(
    secretaryName: String,
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    doctorName: String? = null,
    appointmentDate: String = "",
    appointmentTime: String = ""
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = MediQSpacing.lg,
                start = MediQSpacing.gutter,
                end = MediQSpacing.gutter,
                bottom = MediQSpacing.sm
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .width(MediQSize.minTouchTarget)
                    .height(MediQSize.minTouchTarget)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MediQHeaderControl,
                    modifier = Modifier
                        .width(MediQSize.iconLarge)
                        .height(MediQSize.iconLarge)
                )
            }

            Spacer(Modifier.width(MediQSpacing.md))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = secretaryName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MediQDarkGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (doctorName != null && doctorName.isNotEmpty()) {
                    val doctorLabel = if (doctorName.startsWith("Dr.", ignoreCase = true)) doctorName else "Dr. $doctorName"
                    Text(
                        text = doctorLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MediQTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (appointmentDate.isNotEmpty() || appointmentTime.isNotEmpty()) {
                    val upcoming = listOf(appointmentDate, appointmentTime).filter { it.isNotEmpty() }.joinToString(" · ")
                    Text(
                        text = "Upcoming appointment: $upcoming",
                        style = MaterialTheme.typography.labelMedium,
                        color = MediQTextSecondary,
                        modifier = Modifier.padding(top = MediQSpacing.xxs)
                    )
                }
            }

Spacer(Modifier.width(MediQSpacing.md))
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}
