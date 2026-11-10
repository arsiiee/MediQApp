package com.example.mediq.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.viewmodel.NotificationsViewModel

/**
 * Notifications.
 *
 * Job: read updates. Primary action: open the related screen (handled by tap
 * on a card, not implemented in the mock).
 *
 * Each notification is also promoted to a real Android system notification via
 * [NotificationsViewModel] / [NotificationHelper], so the appointment events
 * surface in the system shade as well as the in-app list.
 *
 * Notifications are loaded from the API via [NotificationsViewModel]; sample
 * data is no longer used in the running app.
 */
@Composable
fun NotificationsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel = viewModel { NotificationsViewModel(context) }
    val notifications = viewModel.notifications.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val error = viewModel.error.collectAsState().value

    val state = when {
        isLoading -> NotificationsUiState.Loading
        error != null -> NotificationsUiState.Error(error.orEmpty())
        notifications.isEmpty() -> NotificationsUiState.Empty
        else -> NotificationsUiState.Content(notifications)
    }

    NotificationsContent(
        state = state,
        actions = NotificationsActions(onRetry = { viewModel.loadNotifications() }),
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
        MediQPageHeader(
            title = "Notifications",
            onBack = onBack
        )

        when (state) {
            NotificationsUiState.Loading -> NotificationsLoadingState()
            is NotificationsUiState.Error -> NotificationsErrorState(message = state.message, onRetry = actions.onRetry)
            NotificationsUiState.Empty -> NotificationsEmptyState()
            is NotificationsUiState.Content -> NotificationsList(items = state.notifications)
        }
    }
}
