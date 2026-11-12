package com.example.mediq.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.viewmodel.NotificationsViewModel

/**
 * Notifications.
 *
 * Opening this screen marks in-app notifications as read in Supabase (best-effort)
 * so unread state does not keep reappearing. Tapping a notification also marks it
 * read and, when linked, opens the related appointment.
 */
@Composable
fun NotificationsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel = viewModel { NotificationsViewModel(context) }
    val notifications = viewModel.notifications.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val error = viewModel.error.collectAsState().value

    // Viewing the inbox should clear unread state in the source of truth once loaded.
    LaunchedEffect(isLoading, notifications.any { it.unread }) {
        if (!isLoading && notifications.any { it.unread }) {
            viewModel.markAllAsRead()
        }
    }

    val state = when {
        isLoading -> NotificationsUiState.Loading
        error != null -> NotificationsUiState.Error(error.orEmpty())
        notifications.isEmpty() -> NotificationsUiState.Empty
        else -> NotificationsUiState.Content(notifications)
    }

    NotificationsContent(
        state = state,
        actions = NotificationsActions(
            onRetry = { viewModel.loadNotifications(markAllRead = true) },
            onOpen = { item ->
                viewModel.markAsRead(item.id)
                item.appointmentId?.takeIf { it.isNotBlank() }?.let { apptId ->
                    navController.navigate(Screen.AppointmentDetails.createRoute(apptId))
                }
            },
        ),
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
            is NotificationsUiState.Error -> NotificationsErrorState(
                message = state.message,
                onRetry = actions.onRetry
            )
            NotificationsUiState.Empty -> NotificationsEmptyState()
            is NotificationsUiState.Content -> NotificationsList(
                items = state.notifications,
                onOpen = actions.onOpen
            )
        }
    }
}
