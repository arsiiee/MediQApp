package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.example.mediq.ui.components.MediQBackHeader
import com.example.mediq.ui.components.MediQStatusMark
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * Registration step 4: the account exists.
 *
 * Job: confirm it happened and point at the door. Primary action: sign in.
 *
 * The header has no back control: there is nothing behind this step worth
 * returning to, and "back" would land on a form for an account that now exists.
 */
@Composable
fun RegisterSuccessScreen(
    navController: NavController,
    actions: RegisterActions = RegisterActions()
) {
    RegisterSuccessContent(
        actions = actions.copy(
            onGoToSignIn = {
                navController.navigate(Screen.SignIn.route) {
                    popUpTo(Screen.RegisterDetails.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    )
}

@Composable
fun RegisterSuccessContent(
    actions: RegisterActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MediQBackground)
    ) {
        MediQBackHeader(title = "Account created", onBack = null)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = MediQSpacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MediQStatusMark(
                icon = Icons.Filled.Check,
                container = MediQLightGreen,
                tint = MediQGreen,
                contentDescription = null
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            Text(
                text = "You're all set",
                style = MaterialTheme.typography.titleLarge,
                color = MediQTextPrimary
            )

            Spacer(Modifier.height(MediQSpacing.xs))

            Text(
                text = "Your account is verified and ready. Sign in to search " +
                        "doctors and book your first consultation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(MediQSpacing.xxl))

            AuthPrimaryButton(
                label = "Continue to sign in",
                onClick = actions.onGoToSignIn
            )
        }

        Spacer(Modifier.height(MediQSpacing.xxl))
    }
}
