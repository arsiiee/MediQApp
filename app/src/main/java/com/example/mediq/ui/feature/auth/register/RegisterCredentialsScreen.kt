package com.example.mediq.ui.feature.auth.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.ui.navigation.Screen

@Composable
fun RegisterCredentialsScreen(
    navController: NavController,
    viewModel: RegisterViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Reaching this step without a verified number means the process was killed
    // between steps 2 and 3, which dropped the registration id held in memory.
    // Sending the user back is the only thing that can recover — `/auth/register`
    // cannot succeed without it.
    LaunchedEffect(Unit) {
        if (!uiState.otpVerified) {
            navController.popBackStack(Screen.RegisterDetails.route, inclusive = false)
        }
    }

    LaunchedEffect(uiState.pendingStep) {
        if (uiState.pendingStep == RegisterStep.SUCCESS) {
            viewModel.onStepHandled()
            navController.navigate(Screen.RegisterSuccess.route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Account credentials",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose the username and password you will use to sign in.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMediQColors.current.secondaryText
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text("Username", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.username,
            onValueChange = viewModel::onUsernameChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            // A username is an identifier, not prose. Autocorrect rewrites the
            // underscore in `demo_patient` into a space and capitalisation turns
            // it into `Demo_patient`; the stored value then stops matching at
            // sign-in. The server normalizes as a backstop, but the field should
            // not send a mangled value in the first place.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalMediQColors.current.accent,
                unfocusedBorderColor = LocalMediQColors.current.outline
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Password", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            visualTransformation = PasswordVisualTransformation(),
            // Without `KeyboardType.Password` Android keeps autocorrect and
            // suggestions live on a secret, so it may rewrite the characters as
            // they are typed — the password then never matches what was set.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalMediQColors.current.accent,
                unfocusedBorderColor = LocalMediQColors.current.outline
            )
        )
        Text(
            text = "At least ${RegisterViewModel.MIN_PASSWORD_LENGTH} characters. Stored securely as a hash.",
            style = MaterialTheme.typography.bodySmall,
            color = LocalMediQColors.current.secondaryText,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Confirm Password", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            visualTransformation = PasswordVisualTransformation(),
            // Same reasoning as the password field above: this one has to receive
            // the identical string, so it must not be autocorrected either.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalMediQColors.current.accent,
                unfocusedBorderColor = LocalMediQColors.current.outline
            )
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = viewModel::register,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MediQOnBrand,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Create Account", fontSize = 18.sp)
            }
        }
    }
}
