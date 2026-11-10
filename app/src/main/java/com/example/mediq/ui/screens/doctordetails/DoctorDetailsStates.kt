package com.example.mediq.ui.screens.doctordetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/*
 * The non-content doctor-details states: error and no slots.
 *
 * The header and the loading skeleton live above and beside them in
 * DoctorDetailsContent, so each state only has to fill what is left and keep
 * the screen's job readable.
 */

/** Recoverable failure: says what happened and offers the retry. */
@Composable
internal fun DoctorDetailsErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
            .padding(MediQSpacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MediQTextSecondary,
            modifier = Modifier.size(MediQSize.control)
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(
            text = "We couldn't load this doctor",
            style = MaterialTheme.typography.titleSmall,
            color = MediQTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(MediQSpacing.sm))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(MediQSpacing.xl))

        Button(
            onClick = onRetry,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.height(MediQSize.minTouchTarget)
        ) {
            Text(
                text = "Try again",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * The profile loaded, but there is nothing to book.
 *
 * The profile stays on screen: the patient already chose this doctor and
 * should not lose what they came for.
 */
@Composable
internal fun DoctorDetailsNoSlotsState(
    doctor: DoctorProfile,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
            .verticalScroll(rememberScrollState())
    ) {
        DoctorProfileCard(doctor = doctor)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MediQSpacing.gutter,
                    vertical = MediQSpacing.xxxl
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MediQTextSecondary,
                modifier = Modifier.size(MediQSize.control)
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            Text(
                text = "No open slots right now",
                style = MaterialTheme.typography.titleSmall,
                color = MediQTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(MediQSpacing.sm))

            Text(
                text = "This doctor's schedule is fully booked. Other days " +
                        "open up as appointments are cancelled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(MediQSpacing.lg))

            Button(
                onClick = onBack,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(MediQSize.minTouchTarget)
            ) {
                Text(
                    text = "Choose another doctor",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(Modifier.height(MediQSpacing.xxl))
    }
}
