package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.Wordmark

/**
 * Splash.
 *
 * Job: name the app and offer the two doors in. Primary action: sign in.
 *
 * It is deliberately thin: the nav graph starts at sign-in, so this screen is
 * only shown if a later change routes through it.
 */
@Composable
fun SplashScreen(navController: NavController) {
    SplashContent(
        onSignIn = { navController.navigate(Screen.SignIn.route) },
        onRegister = { navController.navigate(Screen.RegisterDetails.route) }
    )
}

@Composable
fun SplashContent(
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQGreen)
            .padding(horizontal = MediQSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MediQ",
            style = Wordmark,
            color = MediQOnBrand
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(
            text = "Sign in to book consultations and manage your schedule, " +
                    "or check out our patient appointment system.",
            style = MaterialTheme.typography.bodyMedium,
            color = MediQOnBrand.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(MediQSpacing.xxxl))

        Button(
            onClick = onSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MediQSize.minTouchTarget),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MediQOnBrand,
                contentColor = MediQGreen
            )
        ) {
            Text(text = "Sign in", style = MaterialTheme.typography.labelLarge)
        }

        TextButton(
            onClick = onRegister,
            modifier = Modifier.heightIn(min = MediQSize.minTouchTarget),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Create Account / Register",
                style = MaterialTheme.typography.labelLarge,
                color = MediQOnBrand
            )
        }
    }
}
