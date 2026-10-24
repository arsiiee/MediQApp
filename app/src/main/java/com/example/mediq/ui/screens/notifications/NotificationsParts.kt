package com.example.mediq.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mediq.ui.components.MediQNotice
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/*
 * The pieces that make up the notifications screen.
 */

/** One notification card. */
@Composable
internal fun NotificationCard(
    item: NotificationItem,
    modifier: Modifier = Modifier
) {
    val iconData = NotificationIcons[item.type] ?: NotificationIcons[NotificationType.CONFIRMED]!!

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MediQBackground
    ) {
        Row(
            modifier = Modifier.padding(MediQSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(MediQSize.notificationBadge)
                    .clip(CircleShape)
                    .background(iconData.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconData.icon,
                    contentDescription = null,
                    tint = iconData.tint,
                    modifier = Modifier.size(MediQSize.notificationIcon)
                )
            }

            Spacer(Modifier.width(MediQSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MediQTextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (item.unread) {
                        Box(
                            modifier = Modifier
                                .padding(top = MediQSpacing.xxs)
                                .size(MediQSize.progressBar)
                                .clip(CircleShape)
                                .background(MediQGreen)
                        )
                    }
                }

                Spacer(Modifier.height(MediQSpacing.xxs))

                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MediQTextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(Modifier.height(MediQSpacing.xxs))

                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = com.example.mediq.ui.theme.MediQTextSecondary
                )
            }
        }
    }
}

/** The list of notifications. */
@Composable
internal fun NotificationsList(
    items: List<NotificationItem>,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MediQSpacing.gutter, vertical = MediQSpacing.md),
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.md)
    ) {
        items.forEach { item ->
            NotificationCard(item = item)
        }
    }
}

/** Loading / empty / error states. */
@Composable
internal fun NotificationsLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Loading notifications...", style = MaterialTheme.typography.bodyMedium, color = com.example.mediq.ui.theme.MediQTextSecondary)
    }
}

@Composable
internal fun NotificationsEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(MediQSpacing.lg)
        ) {
            Text(text = "No notifications yet", style = MaterialTheme.typography.titleLarge, color = com.example.mediq.ui.theme.MediQTextPrimary)
            Text(text = "Updates about your appointments will appear here.", style = MaterialTheme.typography.bodyMedium, color = com.example.mediq.ui.theme.MediQTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
internal fun NotificationsErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(MediQSpacing.lg)
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = com.example.mediq.ui.theme.MediQTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = MediQSpacing.gutter))
            androidx.compose.material3.Button(onClick = onRetry, modifier = Modifier.heightIn(min = MediQSize.minTouchTarget)) {
                Text(text = "Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}