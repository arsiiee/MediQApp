package com.example.mediq.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.data.model.ProfileDraft
import com.example.mediq.ui.components.MediQPageHeader
import com.example.mediq.ui.session.MediQSession
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.viewmodel.ProfileViewModel

/**
 * Edit profile.
 *
 * Job: change account details. Primary action: save.
 */
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by MediQSession.user.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()
    val isSaving by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var fullName by rememberSaveable(user?.username) {
        mutableStateOf(user?.fullName ?: MockProfile.fullName)
    }
    var phoneNumber by rememberSaveable(user?.username) {
        mutableStateOf(user?.phoneNumber ?: "")
    }
    var email by rememberSaveable(user?.username) {
        mutableStateOf(user?.email ?: "")
    }

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            val currentUser = user
            if (currentUser != null) {
                MediQSession.rememberAccount(
                    username = currentUser.username,
                    fullName = fullName,
                    email = email,
                    phoneNumber = phoneNumber
                )
                MediQSession.signIn(
                    com.example.mediq.ui.session.SignedInUser(
                        username = currentUser.username,
                        fullName = fullName,
                        email = email.takeIf { it.isNotBlank() },
                        phoneNumber = phoneNumber.takeIf { it.isNotBlank() }
                    )
                )
            }
            navController.popBackStack()
        }
    }

    val state = when {
        updateSuccess -> EditProfileUiState.Saved
        isSaving -> EditProfileUiState.Saving
        error != null -> EditProfileUiState.Error(error.orEmpty())
        else -> EditProfileUiState.Idle
    }

    EditProfileContent(
        state = state,
        fullName = fullName,
        onFullNameChange = { fullName = it },
        phoneNumber = phoneNumber,
        onPhoneNumberChange = { phoneNumber = it },
        email = email,
        actions = EditProfileActions(
            onBack = { navController.popBackStack() },
            onSave = { _, _ ->
                viewModel.resetUpdateSuccess()
                viewModel.updateProfile(
                    ProfileDraft(
                        name = fullName,
                        email = email.takeIf { it.isNotBlank() },
                        mobileNumber = phoneNumber.takeIf { it.isNotBlank() }
                    )
                )
            }
        )
    )
}

/** State switch. Everything below is navigation-free and previewable. */
@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    email: String,
    actions: EditProfileActions,
    modifier: Modifier = Modifier
) {
    val saving = state == EditProfileUiState.Saving

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQPageHeader(
            title = "Edit My Details",
            onBack = actions.onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediQSpacing.gutter, vertical = MediQSpacing.lg)
        ) {
            Text(text = "Your details", style = MaterialTheme.typography.titleLarge, color = MediQTextPrimary)
            Spacer(Modifier.height(MediQSpacing.xs))
            Text(
                text = "Keep your information up to date for your appointments.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary
            )
            Spacer(Modifier.height(MediQSpacing.xxl))

            EditProfileForm(
                fullName = fullName,
                onFullNameChange = onFullNameChange,
                phoneNumber = phoneNumber,
                onPhoneNumberChange = onPhoneNumberChange,
                email = email,
                enabled = !saving
            )

            if (state is EditProfileUiState.Error) {
                Spacer(Modifier.height(MediQSpacing.md))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MediQErrorContainer
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(MediQSpacing.md)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MediQError
                        )
                    }
                }
            }

            Spacer(Modifier.height(MediQSpacing.xxl))

            Button(
                onClick = { actions.onSave(fullName, phoneNumber) },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MediQSize.minTouchTarget),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQGreen,
                    contentColor = MediQOnBrand,
                    disabledContainerColor = com.example.mediq.ui.theme.MediQDisabledContainer,
                    disabledContentColor = MediQOnBrand
                )
            ) {
                Text(
                    text = when (state) {
                        EditProfileUiState.Saving -> "Saving..."
                        EditProfileUiState.Saved -> "Saved"
                        else -> "Save Changes"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}