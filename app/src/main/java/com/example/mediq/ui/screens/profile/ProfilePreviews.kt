package com.example.mediq.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.mediq.ui.theme.MediQTheme

@Preview(name = "Profile - loading", showBackground = true, heightDp = 800)
@Composable
private fun ProfileLoadingPreview() {
    MediQTheme { ProfileContent(state = ProfileSamples.loading, actions = ProfileActions(), showSignOutDialog = false, onDismissSignOut = {}, onConfirmSignOut = {}) }
}

@Preview(name = "Profile - error", showBackground = true, heightDp = 800)
@Composable
private fun ProfileErrorPreview() {
    MediQTheme { ProfileContent(state = ProfileSamples.error, actions = ProfileActions(), showSignOutDialog = false, onDismissSignOut = {}, onConfirmSignOut = {}) }
}

@Preview(name = "Profile - content", showBackground = true, heightDp = 800)
@Composable
private fun ProfileContentPreview() {
    MediQTheme { ProfileContent(state = ProfileSamples.content, actions = ProfileActions(), showSignOutDialog = false, onDismissSignOut = {}, onConfirmSignOut = {}) }
}

@Preview(name = "Profile - sign-out dialog", showBackground = true, heightDp = 500)
@Composable
private fun ProfileSignOutDialogPreview() {
    MediQTheme { ProfileContent(state = ProfileSamples.content, actions = ProfileActions(), showSignOutDialog = true, onDismissSignOut = {}, onConfirmSignOut = {}) }
}

@Preview(name = "Edit profile - idle", showBackground = true, heightDp = 800)
@Composable
private fun EditProfileIdlePreview() {
    MediQTheme { EditProfileContent(state = ProfileSamples.editIdle, fullName = MockProfile.fullName, onFullNameChange = {}, phoneNumber = MockProfile.phoneNumber, onPhoneNumberChange = {}, actions = EditProfileActions()) }
}

@Preview(name = "Edit profile - saving", showBackground = true, heightDp = 800)
@Composable
private fun EditProfileSavingPreview() {
    MediQTheme { EditProfileContent(state = ProfileSamples.editSaving, fullName = MockProfile.fullName, onFullNameChange = {}, phoneNumber = MockProfile.phoneNumber, onPhoneNumberChange = {}, actions = EditProfileActions()) }
}

@Preview(name = "Edit profile - saved", showBackground = true, heightDp = 800)
@Composable
private fun EditProfileSavedPreview() {
    MediQTheme { EditProfileContent(state = ProfileSamples.editSaved, fullName = MockProfile.fullName, onFullNameChange = {}, phoneNumber = MockProfile.phoneNumber, onPhoneNumberChange = {}, actions = EditProfileActions()) }
}

@Preview(name = "Edit profile - error", showBackground = true, heightDp = 800)
@Composable
private fun EditProfileErrorPreview() {
    MediQTheme { EditProfileContent(state = ProfileSamples.editError, fullName = MockProfile.fullName, onFullNameChange = {}, phoneNumber = MockProfile.phoneNumber, onPhoneNumberChange = {}, actions = EditProfileActions()) }
}