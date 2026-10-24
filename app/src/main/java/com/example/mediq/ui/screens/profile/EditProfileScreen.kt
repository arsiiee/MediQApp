package com.example.mediq.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.session.MediQSession
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing

/**
 * Edit profile.
 *
 * Job: change account details. Primary action: save.
 *
 * [state] defaults to the mock source. Replacing it with a repository changes
 * this one call site.
 */
@Composable
fun EditProfileScreen(
    navController: NavController,
    state: EditProfileUiState = ProfileSamples.editIdle
) {
    val user by MediQSession.user.collectAsState()

    /*
     * The form starts from the signed-in patient's own details. The sample
     * fills in only while nobody is signed in; a detail the app does not know
     * starts empty rather than showing somebody else's number.
     */
    var fullName by rememberSaveable(user?.username) {
        mutableStateOf(user?.fullName ?: MockProfile.fullName)
    }
    var phoneNumber by rememberSaveable(user?.username) {
        mutableStateOf(user?.phoneNumber ?: "")
    }

    EditProfileContent(
        state = state,
        fullName = fullName,
        onFullNameChange = { fullName = it },
        phoneNumber = phoneNumber,
        onPhoneNumberChange = { phoneNumber = it },
        actions = EditProfileActions(
            onBack = { navController.popBackStack() },
            onSave = { _, _ -> navController.popBackStack() }
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
    actions: EditProfileActions,
    modifier: Modifier = Modifier
) {
    val saving = state == EditProfileUiState.Saving

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
    ) {
        MediQBackHeader(
            title = "Edit My Details",
            onBack = actions.onBack,
            actions = {}
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediQSpacing.gutter, vertical = MediQSpacing.lg)
        ) {
            Text(text = "Your details", style = MaterialTheme.typography.titleLarge, color = com.example.mediq.ui.theme.MediQTextPrimary)
            Spacer(Modifier.height(MediQSpacing.xs))
            Text(text = "Keep your information up to date for your appointments.", style = MaterialTheme.typography.bodyMedium, color = com.example.mediq.ui.theme.MediQTextSecondary)
            Spacer(Modifier.height(MediQSpacing.xxl))

            EditProfileForm(
                fullName = fullName,
                onFullNameChange = onFullNameChange,
                phoneNumber = phoneNumber,
                onPhoneNumberChange = onPhoneNumberChange,
                enabled = !saving
            )

            if (state is EditProfileUiState.Error) {
                Spacer(Modifier.height(MediQSpacing.md))
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = com.example.mediq.ui.theme.MediQErrorContainer
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(MediQSpacing.md)
                    ) {
                        androidx.compose.material3.Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.example.mediq.ui.theme.MediQError
                        )
                    }
                }
            }

            Spacer(Modifier.height(MediQSpacing.xxl))

            EditProfileSaveButton(
                label = when (state) {
                    EditProfileUiState.Saving -> "Saving..."
                    EditProfileUiState.Saved -> "Saved"
                    else -> "Save Changes"
                },
                onClick = { actions.onSave(fullName, phoneNumber) },
                enabled = !saving
            )
        }
    }
}