package com.example.mediq.ui.feature.auth.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "MediQ",
            color = LocalMediQColors.current.accent,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Sign in",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your role is detected automatically after authentication.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMediQColors.current.secondaryText
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text("Username", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.username,
            onValueChange = { viewModel.onUsernameChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. maria.santos") },
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalMediQColors.current.accent,
                unfocusedBorderColor = LocalMediQColors.current.outline,
                focusedLabelColor = LocalMediQColors.current.accent
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Password", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.password,
            onValueChange = { viewModel.onPasswordChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your password") },
            enabled = !uiState.isLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
            keyboardActions = KeyboardActions(
                onDone = { viewModel.signIn() }
            ),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalMediQColors.current.accent,
                unfocusedBorderColor = LocalMediQColors.current.outline,
                focusedLabelColor = LocalMediQColors.current.accent
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { /* Forgot Password — not yet implemented */ }) {
                Text("Forgot Password?", color = LocalMediQColors.current.accent)
            }
        }

        // Error message surfaced below the password field.
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.signIn() },
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
                Text("Login", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { navController.navigate(Screen.RegisterDetails.route) }) {
            Text("Create Account / Register", color = LocalMediQColors.current.accent)
        }
    }
}