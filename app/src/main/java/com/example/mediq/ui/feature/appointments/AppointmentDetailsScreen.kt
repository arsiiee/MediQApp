package com.example.mediq.ui.feature.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Money
import com.example.mediq.domain.model.toClinicDate
import com.example.mediq.domain.model.toClinicTime
import java.time.format.DateTimeFormatter

private val appointmentDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val appointmentTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/** Matches the `Money.format()` convention in `DoctorsScreen.kt`. */
private fun Money.format(): String = "₱$pesos"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailsScreen(navController: NavController, appointmentId: String?) {
    val viewModel: AppointmentDetailsViewModel =
        viewModel(factory = AppointmentDetailsViewModel.factory(appointmentId))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointment Details", fontWeight = FontWeight.Bold) },
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
        when (val current = uiState.appointment) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LocalMediQColors.current.accent)
                }
            }

            is LoadState.Error -> {
                // Also the state a malformed route lands in — there is no id to
                // fetch, and the message says so rather than spinning forever.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    EmptyState(
                        icon = Icons.Outlined.EventBusy,
                        title = "Appointment not available",
                        description = current.message,
                    )
                }
            }

            is LoadState.Success -> {
                AppointmentDetailsContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding),
                    appointment = current.data,
                    canCancelOrReschedule = uiState.canCancelOrReschedule,
                    actionError = uiState.actionError,
                    onCancel = viewModel::cancel,
                    onReschedule = viewModel::requestReschedule,
                )
            }
        }
    }
}

@Composable
private fun AppointmentDetailsContent(
    modifier: Modifier = Modifier,
    appointment: Appointment,
    canCancelOrReschedule: Boolean,
    actionError: String?,
    onCancel: () -> Unit,
    onReschedule: (String?) -> Unit,
) {
    val colors = LocalMediQColors.current

    // Slot picker state lives here rather than in the ViewModel: it is a
    // short-lived sheet over one screen, and nothing else reads it. What the
    // ViewModel owns is the decision to submit, which the tests pin.
    var rescheduling by remember { mutableStateOf(false) }
    var chosenSlotId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // ── Doctor ────────────────────────────────────────────────────────
        Text(
            text = appointment.doctor.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = appointment.doctor.specialty.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.accent,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatusChip(appointment.status)
        Spacer(modifier = Modifier.height(24.dp))

        // ── Details ───────────────────────────────────────────────────────
        DetailRow(
            icon = { Icon(Icons.Outlined.CalendarMonth, null, tint = colors.accent) },
            label = "When",
            // Clinic zone, never the device's — a booking must not land on the
            // wrong day for someone travelling or on a differently-set phone.
            value = "${appointment.startsAt.toClinicDate().format(appointmentDayFormat)}\n" +
                "${appointment.startsAt.toClinicTime().format(appointmentTimeFormat)}" +
                " – ${appointment.endsAt.toClinicTime().format(appointmentTimeFormat)}",
        )
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(
            icon = { Icon(Icons.Outlined.LocationOn, null, tint = colors.accent) },
            label = "Where",
            value = "${appointment.location.building} · Floor ${appointment.location.floor} · " +
                appointment.location.room,
        )
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(
            icon = { Icon(Icons.Outlined.Payments, null, tint = colors.accent) },
            label = "Fee",
            value = appointment.fee.format(),
        )

        // Only this endpoint returns the reason — the list response omits it, so
        // it is null for anything read off the Appointments tab.
        val reason = appointment.reasonForVisit
        if (!reason.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Reason for visit",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }

        // ── Changes ───────────────────────────────────────────────────────
        // Only for a status the patient can still act on. `isActionable` is
        // false for COMPLETED, CANCELLED, DECLINED, and for UNKNOWN — an
        // unreadable status must not put a mutation on screen.
        if (canCancelOrReschedule) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        rescheduling = false
                        chosenSlotId = null
                        onCancel()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel appointment")
                }
                Button(
                    onClick = { rescheduling = !rescheduling },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (rescheduling) "Close" else "Reschedule")
                }
            }

            if (rescheduling) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Ask to move this appointment",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            // The request does not move it by itself: the clinic
                            // confirms, and the status on screen stays as it is
                            // until they do.
                            text = "The clinic reviews requests during clinic hours. " +
                                "Enter the new time below and we'll send it to them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryText,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = chosenSlotId.orEmpty(),
                            onValueChange = { chosenSlotId = it },
                            label = { Text("Preferred new slot id") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            // Disabled until a slot is named, so the ViewModel's
                            // null guard is a second line of defence rather than
                            // the only one.
                            onClick = {
                                onReschedule(chosenSlotId)
                                rescheduling = false
                            },
                            enabled = !chosenSlotId.isNullOrBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Send request")
                        }
                    }
                }
            }

            if (!actionError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = actionError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = LocalMediQColors.current.secondaryText,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}
