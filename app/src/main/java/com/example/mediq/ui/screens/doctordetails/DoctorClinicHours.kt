package com.example.mediq.ui.screens.doctordetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/** Weekly hours and licence, inside the profile block. */
@Composable
internal fun ClinicHoursCard(
    doctor: DoctorProfile,
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
                text = "Clinic hours",
                style = MaterialTheme.typography.labelMedium,
                color = MediQTextSecondary
            )

            Spacer(Modifier.height(MediQSpacing.sm))

            doctor.clinicHours.forEach { hour ->
                ClinicHourRow(day = hour.day, time = hour.time)
            }

            Spacer(Modifier.height(MediQSpacing.md))

            Text(
                text = "License no. ${doctor.licenseNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MediQTextSecondary
            )
        }
    }
}

@Composable
private fun ClinicHourRow(
    day: String,
    time: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MediQSpacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextPrimary
        )
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextSecondary
        )
    }
}
