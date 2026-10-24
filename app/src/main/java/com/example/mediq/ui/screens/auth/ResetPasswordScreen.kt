package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.data.model.OtpPurpose
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.components.MediQNotice
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Forgot password, step 2: the code and the new password.
 *
 * Job: prove the number belongs to the patient and set a new password.
 * Primary action: reset the password.
 */
@Composable
fun ResetPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val contact = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>(RESET_CONTACT_KEY)
        ?: AuthSamples.PHONE_NUMBER

    ResetPasswordContent(
        contact = contact,
        code = code,
        onCodeChange = { code = it.filter { ch -> ch.isDigit() }.take(AuthValidation.OTP_LENGTH); error = null },
        password = password,
        onPasswordChange = { password = it; error = null },
        confirmation = confirmation,
        onConfirmationChange = { confirmation = it; error = null },
        error = error,
        saving = saving,
        onBack = { navController.popBackStack() },
        onReset = {
            val checked = AuthValidation.validateOtp(code)
            if (checked != null) {
                error = checked
                return@ResetPasswordContent
            }
            if (password != confirmation) {
                error = "Passwords do not match"
                return@ResetPasswordContent
            }
            if (password.length < AuthValidation.MIN_PASSWORD_LENGTH) {
                error = "Use at least ${AuthValidation.MIN_PASSWORD_LENGTH} characters"
                return@ResetPasswordContent
            }

            saving = true
            scope.launch {
                val session = viewModel.verifyOtp(contact, code, OtpPurpose.RESET_PASSWORD)
                val saved = session != null && viewModel.updatePassword(password)
                saving = false
                if (saved) {
                    navController.navigate(Screen.SignIn.route) { popUpTo(0) }
                } else {
                    error = "We could not reset the password. Check the code and try again."
                }
            }
        }
    )
}

/** The code and password step. Everything below this point is previewable. */
@Composable
fun ResetPasswordContent(
    contact: String,
    code: String,
    onCodeChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmation: String,
    onConfirmationChange: (String) -> Unit,
    error: String?,
    saving: Boolean,
    onBack: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQBackground)
    ) {
        MediQBackHeader(title = RESET_TITLE, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MediQSpacing.gutter)
        ) {
            Spacer(Modifier.height(MediQSpacing.xl))

            Text(
                text = "Enter your code",
                style = MaterialTheme.typography.titleLarge,
                color = MediQTextPrimary
            )

            Spacer(Modifier.height(MediQSpacing.xs))

            Text(
                text = "We sent a ${AuthValidation.OTP_LENGTH}-digit code to $contact. Enter it with the new password you want.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.xxl))

            AuthField(
                value = code,
                onValueChange = onCodeChange,
                label = "One-time code",
                placeholder = "123456",
                enabled = !saving,
                error = null
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            AuthPasswordField(
                value = password,
                onValueChange = onPasswordChange,
                label = "New password",
                placeholder = "Your new password",
                enabled = !saving,
                hint = "At least ${AuthValidation.MIN_PASSWORD_LENGTH} characters.",
                error = null
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            AuthPasswordField(
                value = confirmation,
                onValueChange = onConfirmationChange,
                label = "Confirm new password",
                placeholder = "Your new password again",
                enabled = !saving,
                error = null
            )

            if (error != null) {
                Spacer(Modifier.height(MediQSpacing.md))

                MediQNotice(
                    icon = Icons.Outlined.CloudOff,
                    message = error,
                    container = MediQErrorContainer,
                    content = MediQError
                )
            }

            Spacer(Modifier.height(MediQSpacing.xxl))

            AuthPrimaryButton(
                label = if (saving) "Resetting..." else "Reset password",
                enabled = !saving,
                onClick = onReset
            )

            Spacer(Modifier.height(MediQSpacing.xxl))
        }
    }
}
