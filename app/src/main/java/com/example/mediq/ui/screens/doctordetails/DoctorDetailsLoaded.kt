package com.example.mediq.ui.screens.doctordetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediq.data.model.Appointment
import com.example.mediq.ui.session.DateTimeFormatter
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.theme.MediQWarning
import com.example.mediq.ui.theme.MediQWarningContainer

/**
 * The loaded screen: profile block, then the slot picker, then your existing
 * bookings with this doctor, all in one scroll.
 *
 * Selection lives inside [DoctorSlotPicker] — it is view state the patient
 * changes on this screen and never leaves it, so nothing above needs to hear
 * about it.
 */
@Composable
internal fun DoctorDetailsLoaded(
    state: DoctorDetailsUiState.Content,
    actions: DoctorDetailsActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MediQScaffoldBackground)
    ) {
        DoctorProfileCard(doctor = state.doctor)

        // If the patient already has bookings with this doctor, show a banner
        // and disable booking — they cannot double-book the same doctor.
        if (state.userBookings.isNotEmpty()) {
            AlreadyBookedBanner(
                doctorName = state.doctor.name,
                bookings = state.userBookings
            )
            Spacer(Modifier.height(MediQSpacing.lg))
        }

        DoctorSlotPicker(
            schedule = state.schedule,
            onContinue = actions.onContinue,
            modifier = Modifier.fillMaxWidth(),
            alreadyHasBooking = state.userBookings.isNotEmpty()
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        if (state.userBookings.isNotEmpty()) {
            YourBookingsSection(bookings = state.userBookings)
        }

        Spacer(Modifier.height(MediQSpacing.xs))
    }
}

/** Banner shown when the patient already has bookings with this doctor.
 *  Explains that they cannot book another slot with the same doctor. */
@Composable
private fun AlreadyBookedBanner(
    doctorName: String,
    bookings: List<Appointment>
) {
    val latest = bookings.maxByOrNull { it.startsAt }!!
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediQSpacing.gutter)
            .heightIn(min = MediQSize.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = MediQWarningContainer
    ) {
        Column(
            modifier = Modifier
                .padding(MediQSpacing.md)
        ) {
            Text(
                text = "You already have an appointment with $doctorName",
                style = MaterialTheme.typography.titleMedium,
                color = MediQWarning,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "on ${DateTimeFormatter.formatDateLong(latest.startsAt)} at ${DateTimeFormatter.formatTime(latest.startsAt)}. " +
                       "You cannot book another appointment with this doctor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** "Your Bookings" section listing the patient's existing appointments with this doctor. */
@Composable
private fun YourBookingsSection(bookings: List<Appointment>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediQSpacing.gutter)
    ) {
        Text(
            text = "Your Bookings",
            style = MaterialTheme.typography.titleMedium,
            color = MediQTextPrimary
        )

        Spacer(Modifier.height(MediQSpacing.sm))

        bookings.forEach { booking ->
            BookingRow(booking = booking)
            Spacer(Modifier.height(MediQSpacing.xs))
        }
    }
}

/** One row showing a booked appointment's date, time, and status. */
@Composable
private fun BookingRow(booking: Appointment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MediQSpacing.xs)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium)
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = MediQSpacing.md)
        ) {
            Text(
                text = DateTimeFormatter.formatDateLong(booking.startsAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextPrimary
            )
            Text(
                text = DateTimeFormatter.formatTime(booking.startsAt),
                style = MaterialTheme.typography.bodySmall,
                color = MediQTextSecondary
            )
            if (booking.reason.isNotEmpty()) {
                Text(
                    text = booking.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MediQTextSecondary
                )
            }
        }

        Text(
            text = booking.status.name.capitalize(),
            style = MaterialTheme.typography.bodySmall,
            color = MediQTextSecondary
        )
    }
}
