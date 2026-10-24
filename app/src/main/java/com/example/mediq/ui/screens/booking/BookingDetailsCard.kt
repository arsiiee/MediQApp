package com.example.mediq.ui.screens.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQDivider
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/** What is being booked: the doctor, then the chosen day and time. */
@Composable
internal fun BookingDetailsCard(
    summary: BookingSummary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MediQBackground,
        border = BorderStroke(MediQSize.hairline, MediQBorder)
    ) {
        Column(modifier = Modifier.padding(MediQSpacing.lg)) {
            Text(
                text = "BOOKING DETAILS",
                style = MaterialTheme.typography.labelMedium,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.md))

            Text(
                text = summary.doctorName,
                style = MaterialTheme.typography.titleSmall,
                color = MediQTextPrimary
            )

            Text(
                text = summary.specialty,
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.md))

            HorizontalDivider(color = MediQDivider)

            Spacer(Modifier.height(MediQSpacing.md))

            Text(
                text = summary.dateLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MediQTextPrimary
            )

            Spacer(Modifier.height(MediQSpacing.xs))

            Text(
                text = summary.time,
                style = MaterialTheme.typography.labelLarge,
                color = MediQGreen
            )

            Spacer(Modifier.height(MediQSpacing.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MediQTextSecondary,
                    modifier = Modifier.size(MediQSize.icon)
                )

                Spacer(Modifier.width(MediQSpacing.xs))

                Text(
                    text = summary.location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MediQTextSecondary
                )
            }
        }
    }
}
