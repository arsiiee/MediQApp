package com.example.mediq.ui.screens.appointmentdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * Confirms the one irreversible action on this screen.
 *
 * The consequence is spelled out before the destructive button, and the way out
 * (the close control) sits above it, not below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppointmentCancelSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MediQBackground,
        shape = RoundedCornerShape(
            topStart = MediQRadius.lg,
            topEnd = MediQRadius.lg
        ),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MediQSpacing.gutter,
                    vertical = MediQSpacing.sm
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel this appointment?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MediQTextPrimary
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(MediQSize.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close without cancelling",
                        tint = MediQTextSecondary
                    )
                }
            }

            Spacer(Modifier.height(MediQSpacing.xs))

            Text(
                text = "The reserved slot is released straight away so another " +
                        "patient can book it. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MediQSize.minTouchTarget),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQErrorContainer,
                    contentColor = MediQError
                )
            ) {
                Text(
                    text = "Cancel appointment",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(MediQSpacing.xl))
        }
    }
}
