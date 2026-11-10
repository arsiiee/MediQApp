package com.example.mediq.ui.screens.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * What the notifications screen shows.
 */

enum class NotificationType {
    CONFIRMED,
    REMINDER,
    RESCHEDULED,
    CANCELLED
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val time: String,
    val unread: Boolean
)

val MockNotifications = listOf(
    NotificationItem(
        id = "1",
        type = NotificationType.CONFIRMED,
        title = "Appointment confirmed",
        message = "Your consultation with Dr. Maria Elena Sandoval has been confirmed for Monday, Sep 14.",
        time = "2 hrs ago",
        unread = true
    ),
    NotificationItem(
        id = "2",
        type = NotificationType.REMINDER,
        title = "Reminder: Tomorrow",
        message = "Please arrive 15 minutes early for your appointment with Dr. Sandoval tomorrow at 9:30 AM.",
        time = "1 day ago",
        unread = false
    ),
    NotificationItem(
        id = "3",
        type = NotificationType.RESCHEDULED,
        title = "Reschedule approved",
        message = "Your request to reschedule Dr. Kathleen Lim's slot has been approved for Thursday, Sep 17.",
        time = "2 days ago",
        unread = false
    ),
    NotificationItem(
        id = "4",
        type = NotificationType.CANCELLED,
        title = "Appointment cancelled",
        message = "Your booking request for Dr. Ramon Dela Cruz on Sep 11 has been cancelled.",
        time = "3 days ago",
        unread = false
    )
)

/** The icon data for each notification type. */
data class NotificationIcon(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val container: androidx.compose.ui.graphics.Color,
    val tint: androidx.compose.ui.graphics.Color
)

/** Maps a type to its icon + colours. */
val NotificationIcons = mapOf(
    NotificationType.CONFIRMED to NotificationIcon(
        icon = Icons.Filled.CheckCircle,
        container = com.example.mediq.ui.theme.MediQSuccessContainer,
        tint = com.example.mediq.ui.theme.MediQSuccess
    ),
    NotificationType.REMINDER to NotificationIcon(
        icon = Icons.Filled.NotificationsActive,
        container = com.example.mediq.ui.theme.MediQReminderIconContainer,
        tint = com.example.mediq.ui.theme.MediQReminderIconTint
    ),
    NotificationType.RESCHEDULED to NotificationIcon(
        icon = Icons.Filled.Check,
        container = com.example.mediq.ui.theme.MediQSuccessContainer,
        tint = com.example.mediq.ui.theme.MediQSuccess
    ),
    NotificationType.CANCELLED to NotificationIcon(
        icon = Icons.Filled.Cancel,
        container = com.example.mediq.ui.theme.MediQErrorContainer,
        tint = com.example.mediq.ui.theme.MediQError
    )
)

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
    data object Empty : NotificationsUiState
    data class Content(val notifications: List<NotificationItem>) : NotificationsUiState
}

data class NotificationsActions(
    val onRetry: () -> Unit = {}
)