package com.example.mediq.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorBorder
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/** The sign-out confirmation dialog. */
@Composable
internal fun SignOutDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MediQBackground,
            shape = MaterialTheme.shapes.large,
            title = { Text(text = "Sign out?", style = MaterialTheme.typography.titleLarge, color = MediQTextPrimary) },
            text = { Text(text = "Are you sure you want to sign out of your MediQ account?", style = MaterialTheme.typography.bodyMedium, color = MediQTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { onConfirm(); onDismiss() },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MediQError)
                ) {
                    Text(text = "Sign out", style = MaterialTheme.typography.labelLarge, color = MediQOnBrand)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(com.example.mediq.ui.theme.MediQSize.hairline, com.example.mediq.ui.theme.MediQBorder),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MediQTextPrimary)
                ) {
                    Text(text = "Cancel", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

/** Loading / error / empty states for the profile. */
@Composable
internal fun ProfileLoadingState() {
    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        androidx.compose.material3.Text(text = "Loading profile...", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = com.example.mediq.ui.theme.MediQTextSecondary)
    }
}

@Composable
internal fun ProfileErrorState(message: String, onRetry: () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(com.example.mediq.ui.theme.MediQSpacing.lg)) {
            androidx.compose.material3.Text(text = message, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = com.example.mediq.ui.theme.MediQTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = androidx.compose.ui.Modifier.padding(horizontal = com.example.mediq.ui.theme.MediQSpacing.gutter))
            androidx.compose.material3.Button(onClick = onRetry, modifier = androidx.compose.ui.Modifier.heightIn(min = com.example.mediq.ui.theme.MediQSize.minTouchTarget)) {
                androidx.compose.material3.Text(text = "Retry", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            }
        }
    }
}