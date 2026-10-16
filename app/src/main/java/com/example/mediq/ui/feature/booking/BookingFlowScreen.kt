package com.example.mediq.ui.feature.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.component.MediQAvatar
import com.example.mediq.core.designsystem.component.MediQBrandAppBar
import com.example.mediq.core.designsystem.component.MediQButton
import com.example.mediq.core.designsystem.component.MediQDivider
import com.example.mediq.core.designsystem.component.MediQKeyValueGrid
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQAccentLight
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.domain.model.Money
import com.example.mediq.domain.model.toClinicDate
import com.example.mediq.domain.model.toClinicTime
import com.example.mediq.ui.format.formatWithCentavos
import com.example.mediq.ui.navigation.Screen
import java.time.format.DateTimeFormatter

private val bookingDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
private val bookingTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun BookingFlowScreen(navController: NavController, doctorId: String?) {
    val viewModel: BookingViewModel = viewModel(factory = BookingViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selection = BookingSelection.selection

    // Navigate to BookingSuccess as soon as the booking is confirmed by the server.
    LaunchedEffect(uiState.booked) {
        if (uiState.booked) {
            navController.navigate(Screen.BookingSuccess.route) {
                // Pop back to Doctors so pressing Back from Success goes to the list.
                popUpTo(Screen.Doctors.route)
            }
        }
    }

    Scaffold(
        topBar = {
            MediQBrandAppBar(
                title = "Confirm booking",
                onBack = { navController.popBackStack() },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                // The figure's content is taller than a phone, so it scrolls
                // rather than being clipped.
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (selection == null) {
                // Should not normally happen - DoctorDetailsScreen always sets a
                // selection before navigating here.
                Text(
                    text = "No slot selected. Please go back and choose a date and time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                BookingSummary(selection = selection)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Reason for visit (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.reasonForVisit,
                onValueChange = { viewModel.onReasonChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter the reason for your consultation...") },
                enabled = !uiState.isSubmitting,
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = LocalMediQColors.current.accent,
                    unfocusedBorderColor = LocalMediQColors.current.outline
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Only you and clinic staff can read this.",
                style = MaterialTheme.typography.labelSmall,
                color = LocalMediQColors.current.secondaryText,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked  = uiState.confirmedByPatient,
                    onCheckedChange = { viewModel.onConfirmedChanged(it) },
                    enabled  = !uiState.isSubmitting,
                    colors   = CheckboxDefaults.colors(checkedColor = LocalMediQColors.current.accent),
                )
                Text(
                    text  = "I confirm this booking.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            MediQButton(
                text = "Book appointment",
                onClick = { viewModel.submit() },
                enabled = uiState.canSubmit,
                loading = uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            )

            // Why the tap is safe: the claim and the appointment row are one
            // transaction, so nothing here can half-succeed.
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                // The measured pair `MediQAccentLight on MediQLightGreen`
                // (5.89:1) is exactly this card's text on its container.
                color = MediQLightGreen,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your slot is protected at the moment you tap.",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MediQAccentLight,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The write that reserves it and the appointment row run in one " +
                            "transaction, so a confirmed booking and the notification can never " +
                            "disagree.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediQAccentLight,
                    )
                }
            }
        }
    }
}

/** Figure 5's summary: the doctor, the date/time/location facts, then the fee pair. */
@Composable
private fun BookingSummary(selection: BookingSelection.Selection) {
    val colors = LocalMediQColors.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediQAvatar(initials = selection.doctorInitials, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = selection.doctorDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = selection.specialtyDisplayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MediQDivider()
            MediQKeyValueGrid(
                rows = listOf(
                    "DATE" to selection.startsAt.toClinicDate().format(bookingDayFormat),
                    // A range, not just the start: the confirmation is the last
                    // chance to notice a slot that ends when it was not expected to.
                    "TIME" to "${selection.startsAt.toClinicTime().format(bookingTimeFormat)} – " +
                        selection.endsAt.toClinicTime().format(bookingTimeFormat),
                    "LOCATION" to selection.locationDisplay,
                ),
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            AmountRow(
                label  = "Consultation fee",
                amount = Money(selection.feeCentavos).formatWithCentavos(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Care is booked here and paid at the clinic, so nothing changes
            // hands in the app. Zero through the same formatter as the fee, so
            // the two rows cannot drift apart.
            AmountRow(label = "Amount paid at the clinic", amount = Money(0L).formatWithCentavos())
        }
    }
}

@Composable
private fun AmountRow(label: String, amount: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalMediQColors.current.secondaryText,
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun BookingSuccessScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            icon = Icons.Outlined.CheckCircle,
            title = "Appointment booked!",
            description = "Your appointment has been confirmed. Check your appointments for details.",
            actionLabel = "View appointments",
            onAction = { navController.navigate(Screen.Appointments.route) },
        )
    }
}
