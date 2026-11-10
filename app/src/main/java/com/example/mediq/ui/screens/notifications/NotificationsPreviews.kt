package com.example.mediq.ui.screens.notifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

@Preview(name = "Notifications - loading", showBackground = true, heightDp = 800)
@Composable
private fun NotificationsLoadingPreview() {
    MediQTheme { NotificationsContent(state = NotificationsSamples.loading, actions = NotificationsActions(), onBack = {}) }
}

@Preview(name = "Notifications - error", showBackground = true, heightDp = 800)
@Composable
private fun NotificationsErrorPreview() {
    MediQTheme { NotificationsContent(state = NotificationsSamples.error, actions = NotificationsActions(), onBack = {}) }
}

@Preview(name = "Notifications - empty", showBackground = true, heightDp = 800)
@Composable
private fun NotificationsEmptyPreview() {
    MediQTheme { NotificationsContent(state = NotificationsSamples.empty, actions = NotificationsActions(), onBack = {}) }
}

@Preview(name = "Notifications - content", showBackground = true, heightDp = 800)
@Composable
private fun NotificationsContentPreview() {
    MediQTheme { NotificationsContent(state = NotificationsSamples.content, actions = NotificationsActions(), onBack = {}) }
}