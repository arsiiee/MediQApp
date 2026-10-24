package com.example.mediq.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.mediq.ui.theme.MediQHeaderControl
import com.example.mediq.ui.theme.MediQNotificationDot
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing

/**
 * Greeting, patient identity and the notifications entry point.
 *
 * Sits on the brand green header, so every control uses [MediQHeaderControl]
 * or [MediQOnBrand] rather than a second shade of green.
 */
@Composable
fun HomeHeader(
    patient: PatientSummary,
    unreadNotifications: Int,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(
            horizontal = MediQSpacing.gutter,
            vertical = MediQSpacing.md
        ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(patient.initials)

            Spacer(Modifier.width(MediQSpacing.md))

            Column {
                Text(
                    text = "Good day,",
                    style = MaterialTheme.typography.bodySmall,
                    color = MediQOnBrand.copy(alpha = 0.85f)
                )
                Text(
                    text = patient.firstName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MediQOnBrand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        NotificationButton(
            unreadNotifications = unreadNotifications,
            onClick = onNotificationsClick
        )
    }
}

@Composable
private fun InitialsAvatar(initials: String) {
    Box(
        modifier = Modifier
            .size(MediQSize.control)
            .clip(CircleShape)
            .background(MediQHeaderControl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            color = MediQOnBrand
        )
    }
}

/**
 * Notification entry point.
 *
 * The drawn circle is [MediQSize.control]; the touch target is padded up to
 * [MediQSize.minTouchTarget]. The unread state is announced, not just drawn.
 */
@Composable
private fun NotificationButton(
    unreadNotifications: Int,
    onClick: () -> Unit
) {
    val description = if (unreadNotifications > 0) {
        "Notifications, $unreadNotifications unread"
    } else {
        "Notifications"
    }

    Box(
        modifier = Modifier.size(MediQSize.minTouchTarget),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(MediQSize.control)
                .clip(CircleShape)
                .background(MediQHeaderControl)
                .semantics { contentDescription = description }
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MediQOnBrand,
                modifier = Modifier.size(MediQSize.iconLarge)
            )
        }

        if (unreadNotifications > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = MediQSpacing.xs, end = MediQSpacing.xs)
                    .size(MediQSpacing.sm)
                    .clip(CircleShape)
                    .background(MediQNotificationDot)
                    .clearAndSetSemantics { }
            )
        }
    }
}
