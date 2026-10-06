package com.example.mediq.ui.feature.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.core.designsystem.theme.MediQTextSecondary
import com.example.mediq.domain.model.toClinicDate
import com.example.mediq.domain.model.toClinicTime
import com.example.mediq.ui.navigation.Screen
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = { Text("Book Appointment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "BOOKING DETAILS",
                style = MaterialTheme.typography.labelSmall,
                color = LocalMediQColors.current.secondaryText,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (selection == null) {
                // Should not normally happen — DoctorDetailsScreen always sets
                // a selection before navigating here.
                Text(
                    text = "No slot selected. Please go back and choose a date and time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                // ── Booking summary card ──────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MediQLightGreen,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        BookingDetailRow(
                            icon  = Icons.Outlined.MedicalServices,
                            label = selection.doctorDisplayName,
                            sub   = selection.specialtyDisplayName,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BookingDetailRow(
                            icon  = Icons.Outlined.CalendarMonth,
                            label = selection.startsAt.toClinicDate()
                                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BookingDetailRow(
                            icon  = Icons.Outlined.AccessTime,
                            label = selection.startsAt.toClinicTime()
                                .format(DateTimeFormatter.ofPattern("h:mm a")),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BookingDetailRow(
                            icon  = Icons.Outlined.LocationOn,
                            label = selection.locationDisplay,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text  = "Fee: ₱${selection.feeCentavos / 100}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MediQGreen,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Reason for visit (optional)", fontWeight = FontWeight.SemiBold)
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
                    text  = "I confirm that the details above are correct and I wish to book this appointment.",
                    style = MaterialTheme.typography.bodySmall,
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

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick  = { viewModel.submit() },
                enabled  = uiState.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(24.dp),
                        color       = MediQOnBrand,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Confirm Booking", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun BookingDetailRow(
    icon: ImageVector,
    label: String,
    sub: String? = null,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MediQGreen,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    // The light secondary token, not the mode-aware one: this
                    // row always sits on the hardcoded MediQLightGreen card,
                    // which stays light in dark mode. Was Color.Gray, which
                    // measured 3.51:1 on that card.
                    color = MediQTextSecondary,
                )
            }
        }
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