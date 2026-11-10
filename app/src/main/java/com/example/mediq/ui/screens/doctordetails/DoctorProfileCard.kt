package com.example.mediq.ui.screens.doctordetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mediq.ui.theme.MediQAvatarBrandTile
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQSurface
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * The white profile block: who the doctor is, what they charge, when they are in.
 *
 * Full-bleed against the scaffold background, so it reads as one grouped
 * surface rather than a card floating on the screen.
 */
@Composable
internal fun DoctorProfileCard(
    doctor: DoctorProfile,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MediQBackground)
            .padding(
                horizontal = MediQSpacing.gutter,
                vertical = MediQSpacing.lg
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(MediQSize.avatar)
                    .clip(CircleShape)
                    .background(MediQAvatarBrandTile),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doctor.initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MediQTextPrimary
                )
            }

            Spacer(Modifier.width(MediQSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doctor.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MediQTextPrimary
                )

                Spacer(Modifier.height(MediQSpacing.xxs))

                Text(
                    text = doctor.specialty,
                    style = MaterialTheme.typography.labelLarge,
                    color = MediQGreen
                )

                Spacer(Modifier.height(MediQSpacing.xs))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MediQTextSecondary,
                        modifier = Modifier.size(MediQSize.icon)
                    )
                    Spacer(Modifier.width(MediQSpacing.xs))
                    Text(
                        text = doctor.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MediQTextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(MediQSpacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MediQSpacing.md)
        ) {
            DoctorStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AccessTime,
                label = "Experience",
                value = "${doctor.experienceYears} years"
            )
            DoctorStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CalendarMonth,
                label = "Consultation",
                value = "₱${doctor.consultationFee}"
            )
        }

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(
            text = doctor.bio,
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextSecondary
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        ClinicHoursCard(doctor = doctor)
    }
}

/** One figure about the doctor: a labelled pair, not a card in its own right. */
@Composable
private fun DoctorStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MediQSurface
    ) {
        Column(modifier = Modifier.padding(MediQSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MediQTextSecondary,
                    modifier = Modifier.size(MediQSize.icon)
                )
                Spacer(Modifier.width(MediQSpacing.xs))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MediQTextSecondary
                )
            }

            Spacer(Modifier.height(MediQSpacing.xs))

            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MediQTextPrimary
            )
        }
    }
}
