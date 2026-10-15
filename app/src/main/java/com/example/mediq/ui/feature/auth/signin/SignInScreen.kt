package com.example.mediq.ui.feature.auth.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.MediQBrandTextField
import com.example.mediq.core.designsystem.component.MediQButton
import com.example.mediq.core.designsystem.component.MediQDivider
import com.example.mediq.core.designsystem.component.MediQInfoCard
import com.example.mediq.core.designsystem.component.MediQInfoCardTint
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.core.designsystem.theme.MediQOnBrandMuted
import com.example.mediq.ui.navigation.Screen

@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: SignInViewModel = viewModel(factory = SignInViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    // Navigate to Home as soon as sign-in succeeds, clearing the auth stack.
    LaunchedEffect(uiState.signedIn) {
        if (uiState.signedIn) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Fig. 1's header is a full-bleed brand block carrying the wordmark and
        // the tagline, not a themed title on the page background.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MediQGreen)
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                text = "MediQ",
                color = MediQOnBrand,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Book your clinic visit in under a minute",
                color = MediQOnBrandMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            MediQBrandTextField(
                value = uiState.username,
                onValueChange = { viewModel.onUsernameChanged(it) },
                label = "Username",
                placeholder = "e.g. maria.santos",
                enabled = !uiState.isLoading,
                // A username is not prose. Autocorrect rewrites the underscore in
                // `demo_patient` into a space, and capitalisation turns it into
                // `Demo_patient`; either mangles correct credentials into a 401 the
                // server cannot distinguish from a wrong password.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))
            MediQBrandTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = "Password",
                placeholder = "Enter your password",
                enabled = !uiState.isLoading,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                // Without `KeyboardType.Password` Android keeps autocorrect and
                // suggestions live on this field, so it may rewrite the characters
                // as they are typed. `KeyboardType.Password` is also what switches
                // the keyboard to the non-predictive layout. Together these stop a
                // correct password reaching the server as a different string.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.signIn() }),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.Visibility
                            } else {
                                Icons.Filled.VisibilityOff
                            },
                            contentDescription = null,
                        )
                    }
                },
            )

            // The error sentence is the server's, written for a patient to read;
            // the banner is Fig. 1's, the message is not ours to write.
            val error = uiState.error
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                MediQInfoCard(tint = MediQInfoCardTint.Error) {
                    Text(text = error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            MediQButton(
                text = "Sign in",
                onClick = { viewModel.signIn() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                loading = uiState.isLoading,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { /* Forgot password - not yet implemented */ }) {
                    Text("Forgot password?", color = LocalMediQColors.current.accent)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            MediQDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "New to MediQ?",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = LocalMediQColors.current.secondaryText,
                textAlign = TextAlign.Center,
            )
            TextButton(
                onClick = { navController.navigate(Screen.RegisterDetails.route) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Create an account",
                    color = LocalMediQColors.current.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
