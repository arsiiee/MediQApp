package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.viewmodel.BiometricUiState
import com.example.mediq.ui.viewmodel.BiometricViewModel
import kotlinx.coroutines.launch

/**
 * Biometric unlock.
 *
 * Job: prove the person holding the phone is the enrolled owner before the
 * app opens. Shown once per launch when a session already exists; on success
 * the caller navigates to Home.
 */
@Composable
fun BiometricUnlockScreen(
    navController: NavController,
    viewModel: BiometricViewModel = viewModel(factory = BiometricViewModel.Factory(
        androidx.compose.ui.platform.LocalContext.current
    ))
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is BiometricUiState.Success) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.BiometricUnlock.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    BiometricUnlockContent(
        state = uiState,
        onAuthenticate = { viewModel.authenticate() },
        onCancel = { viewModel.cancel() },
        onRetry = { viewModel.authenticate() }
    )
}

/** The unlock UI, driven by [BiometricUiState]. */
@Composable
fun BiometricUnlockContent(
    state: BiometricUiState,
    onAuthenticate: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authenticating = state == BiometricUiState.Authenticating
    val error = state as? BiometricUiState.Error

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQBackground)
            .padding(horizontal = MediQSpacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "Fingerprint",
            modifier = Modifier
                .size(96.dp)
                .padding(bottom = MediQSpacing.xxxl),
            tint = MediQGreen
        )

        Text(
            text = "MediQ requires your biometrics to proceed to the app",
            style = MaterialTheme.typography.titleLarge,
            color = MediQTextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MediQSpacing.xs))

        Text(
            text = "Scan your enrolled fingerprint or face to unlock MediQ.",
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        when (state) {
            is BiometricUiState.Authenticating -> {
                Spacer(modifier = Modifier.height(MediQSpacing.xxl))
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(MediQSize.hero)
                        .padding(bottom = MediQSpacing.md),
                    color = MediQGreen
                )
                Text(
                    text = "Scanning...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediQTextSecondary
                )
            }
            is BiometricUiState.Error -> {
                Spacer(modifier = Modifier.height(MediQSpacing.xxl))
                Text(
                    text = error?.message ?: "Authentication failed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediQTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MediQSpacing.lg))
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .heightIn(min = MediQSize.minTouchTarget),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediQGreen,
                        contentColor = MediQOnBrand
                    )
                ) {
                    Text(text = "Try again", style = MaterialTheme.typography.labelLarge)
                }
            }
            BiometricUiState.Idle -> {
                Spacer(modifier = Modifier.height(MediQSpacing.xxl))
                Button(
                    onClick = onAuthenticate,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .heightIn(min = MediQSize.minTouchTarget),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediQGreen,
                        contentColor = MediQOnBrand
                    )
                ) {
                    Text(text = "Unlock with biometrics", style = MaterialTheme.typography.labelLarge)
                }
            }
            BiometricUiState.Success -> {
                // Handled by LaunchedEffect in the screen; nothing to draw here.
            }
        }
    }
}
