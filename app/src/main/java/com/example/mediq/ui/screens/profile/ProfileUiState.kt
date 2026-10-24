package com.example.mediq.ui.screens.profile

import com.example.mediq.ui.session.DisplayName
import com.example.mediq.ui.session.SignedInUser

/*
 * What the profile and edit-profile screens can be doing.
 */

data class ProfileData(
    val fullName: String,
    val email: String,
    val initials: String,
    val phoneNumber: String
)

val MockProfile = ProfileData(
    fullName = "Arweyne Zoe Salcedo",
    email = "zoe.salcedo@email.com",
    initials = "AZ",
    phoneNumber = "0917 442 8810"
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Content(val profile: ProfileData) : ProfileUiState
}

data class ProfileActions(
    val onEdit: () -> Unit = {},
    val onSignOut: () -> Unit = {}
)

/** Edit profile is simpler: idle, dirty, saving, saved, error. */
sealed interface EditProfileUiState {
    data object Idle : EditProfileUiState
    data class Error(val message: String) : EditProfileUiState
    data object Saving : EditProfileUiState
    data object Saved : EditProfileUiState
}

data class EditProfileActions(
    val onBack: () -> Unit = {},
    val onSave: (String, String) -> Unit = { _, _ -> }
)

/**
 * The state with the signed-in patient's details in it.
 *
 * The sample details are kept for previews and for a screen opened before
 * anyone signs in. Once somebody is signed in, the profile shows that person —
 * and a detail this device does not know is shown as not set, rather than
 * leaking the sample patient's contact details.
 */
fun ProfileUiState.withProfile(user: SignedInUser?): ProfileUiState = when {
    user == null -> this
    this is ProfileUiState.Content -> copy(
        profile = ProfileData(
            fullName = user.fullName,
            email = user.email ?: NOT_SET,
            initials = DisplayName.initials(user.fullName),
            phoneNumber = user.phoneNumber ?: NOT_SET
        )
    )
    else -> this
}

/** Shown in place of a detail the app does not have. */
const val NOT_SET = "Not set"