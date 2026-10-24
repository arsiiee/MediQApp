package com.example.mediq.ui.screens.booking

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.theme.MediQWarning
import com.example.mediq.ui.theme.MediQWarningContainer

/*
 * Both results of the booking: the slot is yours, or it is not.
 *
 * They share the mark, the title and the two button shapes (see
 * BookingResultScaffold.kt) so the screen reads as one place with two
 * outcomes, not two screens glued together.
 */

/** The slot is reserved. */
@Composable
internal fun BookingSuccessBody(
    summary: BookingSummary,
    actions: BookingActions,
    modifier: Modifier = Modifier
) {
    ResultColumn(modifier = modifier) {
        ResultMark(
            icon = Icons.Default.CheckCircle,
            container = MediQLightGreen,
            tint = MediQGreen,
            contentDescription = "Booking successful"
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(
            text = "Your slot is reserved",
            style = MaterialTheme.typography.titleLarge,
            color = MediQTextPrimary
        )

        Spacer(Modifier.height(MediQSpacing.md))

        Text(
            text = "${summary.doctorName} · ${summary.dateLabel} at " +
                    "${summary.time}. The clinic secretary will confirm " +
                    "your appointment, and you will receive a reminder a " +
                    "day before.",
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(MediQSpacing.xxl))

        PrimaryResultButton(
            label = "View my appointments",
            onClick = actions.onViewAppointments
        )

        Spacer(Modifier.height(MediQSpacing.md))

        SecondaryResultButton(
            label = "Add to Google Calendar",
            onClick = actions.onAddToCalendar
        )

        Spacer(Modifier.height(MediQSpacing.xxl))
    }
}

/** The slot went away before the booking landed. */
@Composable
internal fun BookingConflictBody(
    message: String,
    actions: BookingActions,
    modifier: Modifier = Modifier
) {
    ResultColumn(modifier = modifier) {
        ResultMark(
            icon = Icons.Outlined.CalendarMonth,
            container = MediQWarningContainer,
            tint = MediQWarning,
            contentDescription = null
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(
            text = "The slot was taken",
            style = MaterialTheme.typography.titleLarge,
            color = MediQTextPrimary
        )

        Spacer(Modifier.height(MediQSpacing.md))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(MediQSpacing.xxl))

        PrimaryResultButton(
            label = "Choose another slot",
            onClick = actions.onChooseAnotherSlot
        )

        Spacer(Modifier.height(MediQSpacing.xxl))
    }
}
