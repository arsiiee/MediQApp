package com.example.mediq.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQDividerSubtle
import com.example.mediq.ui.theme.MediQElevation
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.theme.MediQWarning
import com.example.mediq.ui.theme.MediQWarningContainer

/**
 * The one thing the patient needs to know: when and where the next visit is.
 *
 * The time is the largest text on the screen ([MaterialTheme.typography.headlineSmall]),
 * everything else is supporting metadata.
 */
@Composable
fun NextConsultationCard(
    appointment: NextAppointment,
    onManageAppointment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MediQBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = MediQElevation.card),
        border = BorderStroke(MediQSize.hairline, MediQBorder)
    ) {
        Column(modifier = Modifier.padding(MediQSpacing.lg)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your next consultation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MediQTextSecondary
                )
                StatusPill(status = appointment.status)
            }

            Spacer(Modifier.height(MediQSpacing.sm))

            Text(
                text = appointment.time,
                style = MaterialTheme.typography.headlineSmall,
                color = MediQTextPrimary
            )
            Text(
                text = appointment.date,
                style = MaterialTheme.typography.bodySmall,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            HorizontalDivider(color = MediQDividerSubtle)

            Spacer(Modifier.height(MediQSpacing.lg))

            Text(
                text = appointment.doctorName,
                style = MaterialTheme.typography.labelLarge,
                color = MediQTextPrimary
            )
            Text(
                text = appointment.specialty,
                style = MaterialTheme.typography.bodySmall,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.sm))

            MetaRow(
                icon = Icons.Default.LocationOn,
                text = appointment.location
            )

            Spacer(Modifier.height(MediQSpacing.xs))

            MetaRow(
                icon = Icons.Default.AccessTime,
                text = appointment.arrivalNote
            )

            Spacer(Modifier.height(MediQSpacing.sm))

            TextButton(
                onClick = { onManageAppointment(appointment.id) },
                modifier = Modifier.heightIn(min = MediQSize.minTouchTarget),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Manage appointment",
                    style = MaterialTheme.typography.labelLarge,
                    color = MediQGreen
                )
                Spacer(Modifier.width(MediQSpacing.xs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MediQGreen,
                    modifier = Modifier.size(MediQSize.icon)
                )
            }
        }
    }
}

/**
 * Status chip.
 *
 * The tint carries meaning only alongside [AppointmentStatus.label], which is
 * always rendered: colour is never the sole indicator.
 */
@Composable
private fun StatusPill(status: AppointmentStatus) {
    val containerColor = when (status) {
        AppointmentStatus.Confirmed -> MediQLightGreen
        AppointmentStatus.Pending -> MediQWarningContainer
        AppointmentStatus.Cancelled -> MediQErrorContainer
    }
    val contentColor = when (status) {
        AppointmentStatus.Confirmed -> MediQGreen
        AppointmentStatus.Pending -> MediQWarning
        AppointmentStatus.Cancelled -> MediQError
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(MediQRadius.pill)
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(
                horizontal = MediQSpacing.md,
                vertical = MediQSpacing.xs
            ),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

