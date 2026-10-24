package com.example.mediq.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQElevation
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * Shown in place of [NextConsultationCard] when nothing is booked.
 *
 * Informational only: the primary "Book a consultation" button sits directly
 * below this card, so a second call to action here would be noise.
 */
@Composable
fun NoUpcomingAppointmentCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MediQBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = MediQElevation.card),
        border = BorderStroke(MediQSize.hairline, MediQBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediQSpacing.lg)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MediQTextSecondary,
                modifier = Modifier.size(MediQSize.control)
            )

            Spacer(Modifier.height(MediQSpacing.md))

            Text(
                text = "No upcoming appointment",
                style = MaterialTheme.typography.labelLarge,
                color = MediQTextPrimary
            )

            Spacer(Modifier.height(MediQSpacing.xs))

            Text(
                text = "When you book a consultation, the time, doctor and clinic room appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MediQTextSecondary
            )
        }
    }
}
