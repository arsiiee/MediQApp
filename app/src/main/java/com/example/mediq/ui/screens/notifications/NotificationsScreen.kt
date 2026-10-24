package com.example.mediq.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSize

/**
 * Notifications.
 *
 * Job: read updates. Primary action: open the related screen (handled by tap
 * on a card, not implemented in the mock).
 *
 * [state] defaults to the mock source. Replacing it with a repository changes
 * this one call site.
 */
@Composable
fun NotificationsScreen(
    navController: NavController,
    state: NotificationsUiState = NotificationsSamples.content
) {
    NotificationsContent(
        state = state,
        actions = NotificationsActions(onRetry = {}),
        onBack = { navController.popBackStack() }
    )
}

/** State switch. Everything below is navigation-free and previewable. */
@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    actions: NotificationsActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQBackHeader(
            title = "Notifications",
            onBack = onBack,
            actions = {}
        )

        when (state) {
            NotificationsUiState.Loading -> NotificationsLoadingState()
            is NotificationsUiState.Error -> NotificationsErrorState(message = state.message, onRetry = actions.onRetry)
            NotificationsUiState.Empty -> NotificationsEmptyState()
            is NotificationsUiState.Content -> NotificationsList(items = state.notifications)
        }
    }
}