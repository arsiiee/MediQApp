package com.example.mediq.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.MediQBrandAppBar
import com.example.mediq.core.designsystem.component.MediQBrandTextField
import com.example.mediq.core.designsystem.component.MediQButton
import com.example.mediq.core.designsystem.component.MediQInfoCard
import com.example.mediq.core.designsystem.component.MediQInfoCardTint
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Sex
import com.example.mediq.ui.navigation.Screen

/**
 * The signed-in user's own profile, editable.
 *
 * The ViewModel is taken from `Screen.Profile`'s back stack entry, the same
 * trick the four register steps use for `RegisterFlow`: the two screens then
 * share one instance, so a save updates the session state the profile screen
 * renders through and popping back shows the new values immediately. A second
 * instance would each hold its own copy, and one of them would be stale.
 *
 * The date of birth is typed, not picked. It is the ISO form the server stores,
 * and the field says so in its supporting text — the registration wizard uses a
 * picker, but a picker here would need its own state and its own tests to move a
 * value the patient can already read off a form.
 */
@Composable
fun EditProfileScreen(navController: NavController) {
    val profileEntry = remember(navController) {
        navController.getBackStackEntry(Screen.Profile.route)
    }
    val viewModel: ProfileViewModel = viewModel(
        viewModelStoreOwner = profileEntry,
        factory = ProfileViewModel.Factory,
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val edit = uiState.edit
    val signedIn = (uiState.session as? LoadState.Success)?.data != null

    Scaffold(
        topBar = {
            MediQBrandAppBar(
                title = "Edit profile",
                onBack = { navController.popBackStack() },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (edit.saveError != null) {
                MediQInfoCard(tint = MediQInfoCardTint.Error) {
                    Text(edit.saveError)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            MediQBrandTextField(
                value = edit.fullName,
                onValueChange = viewModel::onEditFullName,
                label = "Full name",
                placeholder = "e.g. Maria Santos",
                leadingIcon = Icons.Outlined.PersonOutline,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                isError = edit.fullNameError != null,
                supportingText = edit.fullNameError,
            )
            Spacer(modifier = Modifier.height(16.dp))

            MediQBrandTextField(
                value = edit.email,
                onValueChange = viewModel::onEditEmail,
                label = "Email (optional)",
                placeholder = "name@example.com",
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                isError = edit.emailError != null,
                supportingText = edit.emailError,
            )
            Spacer(modifier = Modifier.height(16.dp))

            MediQBrandTextField(
                value = edit.mobileNumber,
                onValueChange = viewModel::onEditMobile,
                label = "Mobile number",
                placeholder = "+639171234567",
                leadingIcon = Icons.Outlined.Phone,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                isError = edit.mobileError != null,
                supportingText = edit.mobileError,
            )
            Spacer(modifier = Modifier.height(16.dp))

            MediQBrandTextField(
                value = edit.dateOfBirth,
                onValueChange = viewModel::onEditDateOfBirth,
                label = "Date of birth",
                placeholder = "1995-06-15",
                leadingIcon = Icons.Outlined.Cake,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                isError = edit.dateOfBirthError != null,
                // The format stays on screen as help until the field refuses a
                // value, at which point the refusal replaces it.
                supportingText = edit.dateOfBirthError ?: "Use the format YYYY-MM-DD.",
            )
            Spacer(modifier = Modifier.height(16.dp))

            SexField(selected = edit.sex, onSelected = viewModel::onEditSex)
            Spacer(modifier = Modifier.height(16.dp))

            MediQBrandTextField(
                value = edit.address,
                onValueChange = viewModel::onEditAddress,
                label = "Address (optional)",
                placeholder = "House, street, city",
                leadingIcon = Icons.Outlined.Home,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
            )
            Spacer(modifier = Modifier.height(24.dp))

            MediQButton(
                text = "Save changes",
                onClick = { viewModel.saveProfile { navController.popBackStack() } },
                loading = edit.isSaving,
                // Reachable only while a session exists; a save with no account
                // has no row to update, and the ViewModel refuses it anyway.
                enabled = signedIn,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * The sex field, as a dropdown: four recorded values plus "not specified".
 *
 * `readOnly` rather than `enabled = false`, so the current value keeps the
 * reading contrast of every other field. A read-only `OutlinedTextField` takes
 * no clicks, which is why the tap target is an invisible box over it — the
 * alternative, `ExposedDropdownMenuBox`, is an experimental API for the same
 * three lines of behaviour.
 */
@Composable
private fun SexField(selected: Sex?, onSelected: (Sex?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Sex (optional)",
            style = MaterialTheme.typography.labelLarge,
            color = LocalMediQColors.current.secondaryText,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selected?.displayName ?: NOT_SPECIFIED,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(NOT_SPECIFIED) },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    },
                )
                Sex.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/** What the field reads when no sex is recorded — the server accepts null. */
private const val NOT_SPECIFIED = "Not specified"
