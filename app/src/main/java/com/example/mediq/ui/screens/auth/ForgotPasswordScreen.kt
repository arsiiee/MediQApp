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
import androidx.compose.ui.text.input.KeyboardType
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

/** The saved-state key the contact step leaves the number under. */
internal const val RESET_CONTACT_KEY = "reset_password_contact"

/**
 * Forgot password, step 1: which number.
 *
 * Job: send a one-time code to the number the patient signed up with.
 * Primary action: send the code.
 */
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var contact by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    ForgotPasswordContent(
        contact = contact,
        onContactChange = { contact = it; error = null },
        error = error,
        sending = sending,
        onBack = { navController.popBackStack() },
        onSendCode = {
            val number = contact.trim()
            if (number.isEmpty()) {
                error = "Enter the number you signed up with"
                return@ForgotPasswordContent
            }
            sending = true
            scope.launch {
                val sent = viewModel.requestOtp(number, OtpPurpose.RESET_PASSWORD)
                sending = false
                if (sent) {
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(RESET_CONTACT_KEY, number)
                    navController.navigate(Screen.ResetPassword.route)
                } else {
                    error = "We could not send the code. Check your connection and try again."
                }
            }
        }
    )
}

/** The number step. Everything below this point is previewable. */
@Composable
fun ForgotPasswordContent(
    contact: String,
    onContactChange: (String) -> Unit,
    error: String?,
    sending: Boolean,
    onBack: () -> Unit,
    onSendCode: () -> Unit,
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
                text = "Reset your password",
                style = MaterialTheme.typography.titleLarge,
                color = MediQTextPrimary
            )

            Spacer(Modifier.height(MediQSpacing.xs))

            Text(
                text = "Tell us the number on your account and we will send a code to prove it is yours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.xxl))

            AuthField(
                value = contact,
                onValueChange = onContactChange,
                label = "Mobile number",
                placeholder = AuthSamples.PHONE_NUMBER,
                error = error,
                enabled = !sending,
                keyboardType = KeyboardType.Phone
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
                label = if (sending) "Sending code..." else "Send code",
                enabled = !sending,
                onClick = onSendCode
            )

            Spacer(Modifier.height(MediQSpacing.xxl))
        }
    }
}

/** The title both reset steps carry. */
internal const val RESET_TITLE = "Reset password"
