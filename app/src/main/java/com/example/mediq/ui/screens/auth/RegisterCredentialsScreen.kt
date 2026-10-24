package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.components.MediQNotice
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.session.MediQSession
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * Registration step 3: choose what the patient will sign in with.
 *
 * Job: set a username and password. Primary action: create the account.
 */
@Composable
fun RegisterCredentialsScreen(
    navController: NavController,
    state: RegisterState = AuthSamples.registerIdle
) {
    RegisterCredentialsContent(
        state = state,
        actions = RegisterActions(
            onBack = { navController.popBackStack() },
            onCredentialsCreate = { username, _ ->
                /*
                 * Create the account now that the username is known, so the
                 * sign-in that follows can greet the patient by the name they
                 * typed at the details step.
                 */
                MediQSession.completeRegistration(username)
                navController.navigate(Screen.RegisterSuccess.route)
            }
        )
    )
}

@Composable
fun RegisterCredentialsContent(
    state: RegisterState,
    actions: RegisterActions,
    initialErrors: CredentialsErrors = CredentialsErrors(null, null, null),
    modifier: Modifier = Modifier
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(initialErrors) }

    val submitting = state == RegisterState.Submitting

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQBackground)
    ) {
        MediQBackHeader(title = REGISTRATION_TITLE, onBack = actions.onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MediQSpacing.gutter)
        ) {
            Spacer(Modifier.height(MediQSpacing.xl))

            RegistrationProgress(currentStep = REGISTRATION_STEPS)

            Spacer(Modifier.height(MediQSpacing.xl))

            Text(
                text = "Account credentials",
                style = MaterialTheme.typography.titleLarge,
                color = MediQTextPrimary
            )

            Spacer(Modifier.height(MediQSpacing.xs))

            Text(
                text = "Choose the username and password you will use to sign in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.xxl))

            AuthField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                placeholder = AuthSamples.USERNAME,
                error = errors.username,
                hint = "Letters, numbers, dots and underscores.",
                enabled = !submitting
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            AuthPasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "Your password",
                error = errors.password,
                hint = "At least ${AuthValidation.MIN_PASSWORD_LENGTH} characters.",
                enabled = !submitting
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            AuthPasswordField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = "Confirm password",
                placeholder = "Your password again",
                error = errors.confirmation,
                enabled = !submitting
            )

            if (state is RegisterState.Error) {
                Spacer(Modifier.height(MediQSpacing.md))

                MediQNotice(
                    icon = Icons.Outlined.ErrorOutline,
                    message = state.message,
                    container = MediQErrorContainer,
                    content = MediQError
                )
            }

            Spacer(Modifier.height(MediQSpacing.xxl))

            AuthPrimaryButton(
                label = if (submitting) "Creating account..." else "Create account",
                enabled = !submitting,
                onClick = {
                    val checked = AuthValidation.validateCredentials(
                        username = username,
                        password = password,
                        confirmation = confirmation
                    )
                    errors = checked
                    if (checked.isClean) {
                        actions.onCredentialsCreate(username, password)
                    }
                }
            )

            Spacer(Modifier.height(MediQSpacing.xxl))
        }
    }
}
