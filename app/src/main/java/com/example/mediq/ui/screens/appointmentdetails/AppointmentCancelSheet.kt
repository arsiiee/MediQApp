package com.example.mediq.ui.screens.appointmentdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

@Composable
internal fun AppointmentCancelSheet(
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cancel this appointment?",
                style = MaterialTheme.typography.titleMedium,
                color = MediQTextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tell the clinic why (optional).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediQTextSecondary
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(text = "Reason for cancelling") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 4,
                    minLines = 2,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQError,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Cancel appointment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep appointment")
            }
        }
    )
}
