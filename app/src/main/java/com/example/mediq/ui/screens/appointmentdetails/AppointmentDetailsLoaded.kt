package com.example.mediq.ui.screens.appointmentdetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mediq.ui.screens.appointments.AppointmentBooking
import com.example.mediq.ui.screens.appointments.AppointmentStatus
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorBorder
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQWarning

/**
 * One booking, loaded.
 *
 * The cancel control only appears when the booking can still be cancelled, so
 * the screen never offers an action it cannot honour.
 */
@Composable
internal fun AppointmentDetailsLoaded(
    booking: AppointmentBooking,
    actions: AppointmentDetailsActions,
    modifier: Modifier = Modifier
) {
    var showCancelSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = MediQSpacing.gutter,
                vertical = MediQSpacing.lg
            )
    ) {
        CurrentStatusRow(status = booking.status)

        Spacer(Modifier.height(MediQSpacing.lg))

        AppointmentInfoCard(booking = booking)

        Spacer(Modifier.height(MediQSpacing.lg))

        AppointmentMapPreview(location = booking.location)

        Spacer(Modifier.height(MediQSpacing.lg))

        Button(
            onClick = { actions.onChatWithSecretary(booking.conversationId) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MediQSize.minTouchTarget),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MediQGreen,
                contentColor = MediQOnBrand
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(MediQSize.iconLarge)
            )

            Spacer(Modifier.width(MediQSpacing.sm))

            Text(
                text = "Chat with Secretary",
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (booking.status.canCancel) {
            Spacer(Modifier.height(MediQSpacing.md))

            OutlinedButton(
                onClick = { showCancelSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MediQSize.minTouchTarget),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(MediQSize.hairline, MediQErrorBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MediQErrorContainer,
                    contentColor = MediQError
                )
            ) {
                Text(
                    text = "Cancel appointment",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(Modifier.height(MediQSpacing.xxl))
    }

    if (showCancelSheet) {
        AppointmentCancelSheet(
            onDismiss = { showCancelSheet = false },
            onConfirm = {
                showCancelSheet = false
                actions.onCancelAppointment(booking.id)
            }
        )
    }
}

/** The status line above the card. Its label carries the state, not the colour. */
@Composable
private fun CurrentStatusRow(status: AppointmentStatus) {
    val tone = when (status) {
        AppointmentStatus.Confirmed,
        AppointmentStatus.Completed -> MediQGreen

        AppointmentStatus.AwaitingConfirmation -> MediQWarning
        AppointmentStatus.Cancelled -> MediQError
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Current status",
            style = MaterialTheme.typography.labelLarge,
            color = MediQTextPrimary
        )

        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = tone
        )
    }
}
