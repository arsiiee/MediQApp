package com.example.mediq.ui.screens.appointments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mediq.ui.theme.MediQAvatarBrandTile
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQDivider
import com.example.mediq.ui.theme.MediQElevation
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQNeutralContainer
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * One booking in the list.
 *
 * The whole card is the target and names the booking it opens, so a screen
 * reader does not read the doctor and then leave the patient to guess what
 * tapping does.
 */
@Composable
internal fun AppointmentCard(
    booking: AppointmentBooking,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Open the appointment with ${booking.doctor}",
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MediQBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = MediQElevation.card),
        border = BorderStroke(MediQSize.hairline, MediQBorder)
    ) {
        Column(modifier = Modifier.padding(MediQSpacing.lg)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                InitialsAvatar(initials = booking.initials)

                Spacer(Modifier.width(MediQSpacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.doctor,
                        style = MaterialTheme.typography.labelLarge,
                        color = MediQTextPrimary
                    )
                    Text(
                        text = booking.specialty,
                        style = MaterialTheme.typography.labelMedium,
                        color = MediQTextSecondary
                    )
                }

                Spacer(Modifier.width(MediQSpacing.sm))

                AppointmentStatusChip(status = booking.status)
            }

            Spacer(Modifier.height(MediQSpacing.md))

            HorizontalDivider(color = MediQDivider)

            Spacer(Modifier.height(MediQSpacing.md))

            DetailRow(
                icon = Icons.Outlined.CalendarMonth,
                text = "${booking.dateShort} · ${booking.time}",
                emphasis = true
            )

            Spacer(Modifier.height(MediQSpacing.sm))

            DetailRow(
                icon = Icons.Outlined.LocationOn,
                text = booking.location,
                emphasis = false
            )

            Spacer(Modifier.height(MediQSpacing.md))

            ReasonChip(reason = booking.reason)
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    text: String,
    emphasis: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MediQTextSecondary,
            modifier = Modifier.size(MediQSize.icon)
        )

        Spacer(Modifier.width(MediQSpacing.sm))

        Text(
            text = text,
            style = if (emphasis) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
            color = if (emphasis) MediQTextPrimary else MediQTextSecondary
        )
    }
}

@Composable
private fun ReasonChip(reason: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MediQNeutralContainer
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MediQSpacing.md,
                vertical = MediQSpacing.sm
            )
        ) {
            Text(
                text = "Reason:",
                style = MaterialTheme.typography.labelMedium,
                color = MediQTextPrimary
            )

            Spacer(Modifier.width(MediQSpacing.xs))

            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MediQTextSecondary
            )
        }
    }
}

@Composable
private fun InitialsAvatar(initials: String) {
    Box(
        modifier = Modifier
            .size(MediQSize.avatar)
            .clip(CircleShape)
            .background(MediQAvatarBrandTile),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            color = MediQGreen
        )
    }
}
