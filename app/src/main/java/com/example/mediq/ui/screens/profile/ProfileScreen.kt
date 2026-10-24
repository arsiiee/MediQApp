package com.example.mediq.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.data.api.RetrofitClient
import com.example.mediq.ui.session.MediQSession
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSpacing

/**
 * Profile.
 *
 * Job: manage account and health data. Primary action: edit profile.
 *
 * [state] defaults to the mock source. Replacing it with a repository changes
 * this one call site.
 */
@Composable
fun ProfileScreen(
    navController: NavController,
    state: ProfileUiState = ProfileSamples.content
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    val user by MediQSession.user.collectAsState()

    ProfileContent(
        state = state.withProfile(user),
        actions = ProfileActions(
            onEdit = { navController.navigate(Screen.EditProfile.route) },
            onSignOut = { showSignOutDialog = true }
        ),
        showSignOutDialog = showSignOutDialog,
        onDismissSignOut = { showSignOutDialog = false },
        onConfirmSignOut = {
            showSignOutDialog = false
            MediQSession.signOut()
            RetrofitClient.accessToken = null
            navController.navigate(Screen.SignIn.route) { popUpTo(0) }
        }
    )
}

/** State switch. Everything below is navigation-free and previewable. */
@Composable
fun ProfileContent(
    state: ProfileUiState,
    actions: ProfileActions,
    showSignOutDialog: Boolean,
    onDismissSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        when (state) {
            ProfileUiState.Loading -> ProfileLoadingState()
            is ProfileUiState.Error -> ProfileErrorState(message = state.message, onRetry = {})
            is ProfileUiState.Content -> {
                ProfileHeader(profile = state.profile)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = MediQSpacing.gutter)
                ) {
                    Spacer(Modifier.height(MediQSpacing.lg))

                    ProfileDetailsCard(profile = state.profile, onEdit = actions.onEdit)

                    Spacer(Modifier.height(MediQSpacing.md))

                    SignOutButton(onClick = actions.onSignOut)
                }
            }
        }

        SignOutDialog(
            show = showSignOutDialog,
            onDismiss = onDismissSignOut,
            onConfirm = onConfirmSignOut
        )
    }
}