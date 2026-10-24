package com.example.mediq.ui.screens.appointmentdetails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.mediq.ui.screens.appointments.AppointmentBooking
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQDivider
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/** The booking, field by field: who, when, where, why. */
@Composable
internal fun AppointmentInfoCard(
    booking: AppointmentBooking,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MediQBackground,
        border = BorderStroke(MediQSize.hairline, MediQBorder)
    ) {
        Column(modifier = Modifier.padding(MediQSpacing.lg)) {

            DetailField(label = "Doctor", value = booking.doctor)
            Text(
                text = booking.specialty,
                style = MaterialTheme.typography.labelMedium,
                color = MediQGreen
            )

            FieldDivider()

            DetailField(
                label = "When",
                value = "${booking.dateLong} · ${booking.time}"
            )

            FieldDivider()

            DetailField(label = "Where", value = booking.location)

            FieldDivider()

            DetailField(label = "Reason", value = booking.reason)
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MediQTextSecondary
    )

    Spacer(Modifier.height(MediQSpacing.xxs))

    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MediQTextPrimary
    )
}

@Composable
private fun FieldDivider() {
    Spacer(Modifier.height(MediQSpacing.md))
    HorizontalDivider(color = MediQDivider)
    Spacer(Modifier.height(MediQSpacing.md))
}

/**
 * Placeholder for the clinic map.
 *
 * Not interactive yet, so it announces itself as a map of the clinic rather
 * than pretending to be a tappable control.
 */
@Composable
internal fun AppointmentMapPreview(
    location: String = "ACE Medical Center",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val query = Uri.encode(location)
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:0,0?q=$query")
                )
                // Only launch if something can handle it; otherwise do nothing
                // rather than crashing on devices without a maps app.
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            },
        shape = MaterialTheme.shapes.large,
        color = MediQLightGreen
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MediQSize.mapPreview)
                .padding(MediQSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = null,
                tint = MediQGreen,
                modifier = Modifier.size(MediQSize.control)
            )

            Spacer(Modifier.width(MediQSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location,
                    style = MaterialTheme.typography.labelLarge,
                    color = MediQGreen
                )
                Text(
                    text = "Tap to open in Google Maps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediQTextSecondary
                )
            }
        }
    }
}
