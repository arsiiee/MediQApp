package com.example.mediq.ui.screens.notifications

/*
 * Named states for notifications.
 */
object NotificationsSamples {

    val loading: NotificationsUiState = NotificationsUiState.Loading

    val error: NotificationsUiState = NotificationsUiState.Error(
        "We could not load your notifications. Check your connection and try again."
    )

    val empty: NotificationsUiState = NotificationsUiState.Empty

    val content: NotificationsUiState = NotificationsUiState.Content(MockNotifications)
}