package com.example.mediq.ui.screens.profile

/*
 * Named states for profile and edit-profile.
 */
object ProfileSamples {

    // Profile

    val loading: ProfileUiState = ProfileUiState.Loading

    val error: ProfileUiState = ProfileUiState.Error(
        "We could not load your profile. Check your connection and try again."
    )

    val content: ProfileUiState = ProfileUiState.Content(MockProfile)

    // Edit profile

    val editIdle: EditProfileUiState = EditProfileUiState.Idle
    val editSaving: EditProfileUiState = EditProfileUiState.Saving
    val editSaved: EditProfileUiState = EditProfileUiState.Saved
    val editError: EditProfileUiState = EditProfileUiState.Error(
        "We could not save your changes. Check your connection and try again."
    )
}