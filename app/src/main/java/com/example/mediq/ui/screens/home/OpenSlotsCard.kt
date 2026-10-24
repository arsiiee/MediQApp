package com.example.mediq.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQDivider
import com.example.mediq.ui.theme.MediQElevation
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQIconMuted
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * Doctors with the most open slots this week.
 *
 * Availability is a number, not a colour, so it stays legible without colour
 * perception. Each row announces its own destination.
 */
@Composable
fun OpenSlotsCard(
    doctors: List<DoctorAvailability>,
    onDoctorClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

        SectionTitle(text = "Most open slots this week")

        Spacer(Modifier.height(MediQSpacing.md))

        if (doctors.isEmpty()) {
            EmptySlotsCard()
            return@Column
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MediQBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = MediQElevation.none),
            border = BorderStroke(MediQSize.hairline, MediQBorder)
        ) {
            doctors.forEachIndexed { index, doctor ->

                DoctorAvailabilityRow(
                    doctor = doctor,
                    onClick = { onDoctorClick(doctor.doctorId) }
                )

                if (index < doctors.lastIndex) {
                    HorizontalDivider(color = MediQDivider)
                }
            }
        }
    }
}

@Composable
private fun DoctorAvailabilityRow(
    doctor: DoctorAvailability,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "View ${doctor.name}",
                onClick = onClick
            )
            .heightIn(min = MediQSize.minTouchTarget)
            .padding(
                horizontal = MediQSpacing.lg,
                vertical = MediQSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doctor.name,
                style = MaterialTheme.typography.labelLarge,
                color = MediQTextPrimary
            )
            Spacer(Modifier.height(MediQSpacing.xxs))
            Text(
                text = "${doctor.specialty} · ${doctor.location}",
                style = MaterialTheme.typography.labelSmall,
                color = MediQTextSecondary
            )
        }

        Text(
            text = doctor.openSlots.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MediQGreen
        )

        Spacer(Modifier.width(MediQSpacing.sm))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MediQIconMuted,
            modifier = Modifier.size(MediQSize.icon)
        )
    }
}

/** Shown when no doctor has availability loaded this week. */
@Composable
private fun EmptySlotsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MediQSpacing.lg,
                vertical = MediQSpacing.xl
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MediQSpacing.xs)
        ) {
            Text(
                text = "No open slots this week",
                style = MaterialTheme.typography.labelLarge,
                color = MediQTextPrimary
            )
            Text(
                text = "Browse all doctors to see the next available dates.",
                style = MaterialTheme.typography.bodySmall,
                color = MediQTextSecondary
            )
        }
    }
}
