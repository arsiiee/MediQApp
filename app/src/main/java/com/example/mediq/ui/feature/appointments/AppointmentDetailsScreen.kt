package com.example.mediq.ui.feature.appointments

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.component.MediQAvatar
import com.example.mediq.core.designsystem.component.MediQBrandAppBar
import com.example.mediq.core.designsystem.component.MediQButton
import com.example.mediq.core.designsystem.component.MediQButtonVariant
import com.example.mediq.core.designsystem.component.MediQInfoCard
import com.example.mediq.core.designsystem.component.MediQInfoCardTint
import com.example.mediq.core.designsystem.component.MediQKeyValueGrid
import com.example.mediq.core.designsystem.component.MediQStatusChip
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.model.toClinicDate
import com.example.mediq.domain.model.toClinicTime
import com.example.mediq.ui.feature.booking.SlotPicker
import com.example.mediq.ui.format.formatWithCentavos
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Figure 6's `DATE` and `TIME` rows, in the clinic's zone — never the device's. */
private val appointmentDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
private val appointmentTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Figure 6's `BOOKED ON 07 Oct 2026`. */
private val bookedOnFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@Composable
fun AppointmentDetailsScreen(navController: NavController, appointmentId: String?) {
    val viewModel: AppointmentDetailsViewModel =
        viewModel(factory = AppointmentDetailsViewModel.factory(appointmentId))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            // The status lives here rather than in the body because figure 6
            // draws it top-right of the green bar. It stays `MediQStatusChip`
            // (not a pill styled for the bar) because that component is the one
            // place the six status pairs are mapped and the one
            // `check-contrast.ps1` parses.
            MediQBrandAppBar(
                title = "Appointment",
                onBack = { navController.popBackStack() },
                actions = {
                    (uiState.appointment as? LoadState.Success)?.data?.let {
                        MediQStatusChip(it.status)
                    }
                },
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
                    contentAlignment = Alignment.Center,
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
                        .padding(innerPadding),
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
                    state = uiState,
                    onCancel = viewModel::cancel,
                    onOpenReschedule = viewModel::openReschedulePicker,
                    onCloseReschedule = viewModel::closeReschedulePicker,
                    onDateSelected = viewModel::onDateSelected,
                    onSlotSelected = viewModel::onSlotSelected,
                    onSendReschedule = viewModel::requestReschedule,
                )
            }
        }
    }
}

@Composable
private fun AppointmentDetailsContent(
    modifier: Modifier = Modifier,
    appointment: Appointment,
    state: AppointmentDetailsUiState,
    onCancel: () -> Unit,
    onOpenReschedule: () -> Unit,
    onCloseReschedule: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onSlotSelected: (TimeSlot) -> Unit,
    onSendReschedule: () -> Unit,
) {
    val colors = LocalMediQColors.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // ── Doctor card ────────────────────────────────────────────────────
        // Figure 6 draws this white on white, separated only by a hairline, so
        // the fill is a token (`background`) and never `Color.White` — the
        // literal is what the contrast gate exists to keep out, and in dark
        // mode a hardcoded white card would carry theme text at 1.00:1.
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediQAvatar(initials = appointment.doctor.initials, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = appointment.doctor.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${appointment.doctor.specialty.displayName} · " +
                            "PRC ${appointment.doctor.licenseNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Facts ──────────────────────────────────────────────────────────
        // The figure lays these out in two columns; `MediQKeyValueGrid` draws
        // them as one label/value row per pair, which is the same resolution
        // figure 5's grid was already given in `ui-scheduling`. Two screens
        // drawing the same facts differently would cost more recognisability
        // than either column split buys.
        val facts = mutableListOf(
            "DATE" to appointment.startsAt.toClinicDate().format(appointmentDayFormat),
            "TIME" to "${appointment.startsAt.toClinicTime().format(appointmentTimeFormat)} - " +
                appointment.endsAt.toClinicTime().format(appointmentTimeFormat),
            // The seed stores `2F`, so the old `"Floor $floor"` prefix rendered
            // "Floor 2F". The three fields are joined verbatim, as the figure
            // writes them.
            "LOCATION" to appointment.location.let {
                "${it.building} · ${it.floor} · ${it.room}"
            },
            "FEE" to appointment.fee.formatWithCentavos(),
            "STATUS" to appointment.status.displayName,
        )
        // Null when the server does not send the field or sends one this build
        // cannot read (both arrive as null through Gson). The row is dropped
        // rather than drawn empty — "unknown" is the honest rendering, and the
        // rest of the appointment still reads.
        appointment.createdAt?.let {
            facts += "BOOKED ON" to it.toClinicDate().format(bookedOnFormat)
        }
        MediQKeyValueGrid(rows = facts)

        // ── Reason for visit ───────────────────────────────────────────────
        // Only this endpoint returns the reason — the list response omits it, so
        // it is null for anything read off the Appointments tab.
        val reason = appointment.reasonForVisit
        if (!reason.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "REASON FOR VISIT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.secondaryText,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Returned only by this endpoint, never by the list.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryText,
                    )
                }
            }
        }

        // ── Changes ────────────────────────────────────────────────────────
        // Only for a status the patient can still act on. `isActionable` is
        // false for COMPLETED, CANCELLED, DECLINED, and for UNKNOWN — an
        // unreadable status must not put a mutation on screen.
        if (state.canCancelOrReschedule) {
            Spacer(modifier = Modifier.height(20.dp))

            // Toggling rather than a sticky flag: `openReschedulePicker` and
            // `closeReschedulePicker` also clear the chosen slot, so reopening
            // the picker never starts on the slot the patient picked last time.
            MediQButton(
                text = if (state.isReschedulePickerOpen) "Close" else "Request a reschedule",
                onClick = {
                    if (state.isReschedulePickerOpen) {
                        onCloseReschedule()
                    } else {
                        onOpenReschedule()
                    }
                },
                variant = MediQButtonVariant.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))

            // The same outlined shape in the error role: figure 6 draws cancel
            // in `#B00020`, and `MaterialTheme.colorScheme.error` is the
            // mode-dependent form of it the gate already measures in both modes
            // (`light error on raised`, `dark error on background`).
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(
                    text = "Cancel this appointment",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (state.isReschedulePickerOpen) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ask to move this appointment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    // The request does not move it by itself: the clinic confirms,
                    // and the status on screen stays as it is until they do.
                    text = "Pick a new time below and we'll send the request to the clinic. " +
                        "They review requests during clinic hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
                Spacer(modifier = Modifier.height(12.dp))

                SlotPicker(
                    availableDates = state.availableDates,
                    selectedDate = state.selectedDate,
                    slots = state.slots,
                    selectedSlot = state.selectedSlot,
                    onDateSelected = onDateSelected,
                    onSlotSelected = onSlotSelected,
                    datesTitle = "New dates",
                    slotsTitle = "New times",
                )

                Spacer(modifier = Modifier.height(16.dp))
                MediQButton(
                    // Disabled until a slot is chosen, so the ViewModel's
                    // no-slot guard is a second line of defence rather than the
                    // only one.
                    text = "Send request",
                    onClick = onSendReschedule,
                    enabled = state.canSendRescheduleRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            }

            val actionError = state.actionError
            if (!actionError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = actionError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // ── Why the buttons may be missing ─────────────────────────────────
        // Deliberately *outside* the branch above: figure 6 draws it
        // unconditionally, and it earns its place exactly when the buttons are
        // gone — a cancelled, completed or declined appointment explains itself
        // instead of showing an empty stretch. The card sets its own content
        // colour, so every line here is the measured `MediQWarningOnContainer on
        // MediQWarningContainer` pair (5.96:1) rather than a token picked per
        // Text.
        Spacer(modifier = Modifier.height(20.dp))
        MediQInfoCard(tint = MediQInfoCardTint.Warning) {
            Text(
                text = "Why you may not see these buttons",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "They are driven by one rule — the status permits a change.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cancelled, completed or declined shows no action at all, so " +
                    "the app never offers an action the server would refuse.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Cancelling frees the slot so another patient can take it.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
