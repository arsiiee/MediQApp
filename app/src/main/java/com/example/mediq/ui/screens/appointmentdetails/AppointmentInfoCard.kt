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
import android.content.ActivityNotFoundException
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
    val hasLocation = location.isNotBlank()

    /** The hospital/building name only — floor and room details are for display,
     * not for navigation. Split on the first separator (" · " or " — ") so the
     * map goes to the building, not a room inside it. */
    val mapLocation = location
        .split(Regex("[ —·]"))
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: location

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = hasLocation,
                onClick = {
                    val query = Uri.encode(mapLocation)
                    // 1) Try Google Maps app directly (most reliable on devices that have it).
                    val mapsAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")).apply {
                        setPackage("com.google.android.apps.maps")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(mapsAppIntent)
                        return@clickable
                    } catch (_: ActivityNotFoundException) {}

                    // 2) Try any app that handles geo: (e.g. Apple Maps, Waze on some devices).
                    val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(geoIntent)
                        return@clickable
                    } catch (_: ActivityNotFoundException) {}

                    // 3) Fallback: Google Maps search page in a browser.
                    // Use the plain search URL (not maps.google.com) so it stays
                    // in the browser instead of redirecting to an intent:// scheme
                    // that browsers can't load.
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(browserIntent)
                }
            ),
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
