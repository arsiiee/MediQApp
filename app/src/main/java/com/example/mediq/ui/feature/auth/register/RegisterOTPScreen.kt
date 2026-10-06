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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.ui.navigation.Screen

@Composable
fun RegisterOTPScreen(
    navController: NavController,
    viewModel: RegisterViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Only a verified code moves the wizard on. The button used to navigate
    // unconditionally, so any six digits reached the credentials step.
    LaunchedEffect(uiState.pendingStep) {
        if (uiState.pendingStep == RegisterStep.CREDENTIALS) {
            viewModel.onStepHandled()
            navController.navigate(Screen.RegisterCredentials.route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Verify your number",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (uiState.mobileNumber.isBlank()) {
                "We sent a 6-digit code to your number"
            } else {
                "We sent a 6-digit code to ${uiState.mobileNumber}"
            },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMediQColors.current.secondaryText
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text("One-time code", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.otp,
            onValueChange = viewModel::onOtpChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("6 digits") },
            enabled = !uiState.isLoading,
            // An OTP is a credential. `NumberPassword` is the numeric keypad
            // with autocorrect and suggestions disabled — `Number` alone would
            // still leave the keyboard free to rewrite the digits.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalMediQColors.current.accent,
                unfocusedBorderColor = LocalMediQColors.current.outline
            )
        )

        // There is no SMS provider yet, so the server returns the code it
        // generated (`AuthService.returnCodeToCaller`). Shown as a hint, never
        // required — a real provider sends nothing back and the field still
        // works.
        if (uiState.devOtpHint.isNotBlank()) {
            Text(
                text = "Development only — your code is ${uiState.devOtpHint}",
                style = MaterialTheme.typography.bodySmall,
                color = LocalMediQColors.current.secondaryText,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

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
            onClick = viewModel::verifyOtp,
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
                Text("Verify", fontSize = 18.sp)
            }
        }
    }
}
