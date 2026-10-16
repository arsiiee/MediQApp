package com.example.mediq.ui.feature.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.component.MediQAvatar
import com.example.mediq.core.designsystem.component.MediQBrandAppBar
import com.example.mediq.core.designsystem.component.MediQButton
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQAccentLight
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.ClinicHours
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.ui.feature.booking.BookingSelection
import com.example.mediq.ui.feature.booking.MonthStepper
import com.example.mediq.ui.feature.booking.SlotPicker
import com.example.mediq.ui.format.formatWithCentavos
import com.example.mediq.ui.navigation.Screen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DoctorDetailsScreen(navController: NavController, doctorId: String?) {
    val viewModel: DoctorDetailsViewModel =
        viewModel(factory = DoctorDetailsViewModel.factory(doctorId))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            MediQBrandAppBar(
                title = "Doctor details",
                onBack = { navController.popBackStack() },
            )
        },
        bottomBar = {
            val doctor = (uiState.doctor as? LoadState.Success)?.data
            val slot = uiState.selectedSlot
            if (doctor != null && slot != null) {
                MediQButton(
                    text = "Continue to booking",
                    onClick = {
                        BookingSelection.set(
                            BookingSelection.Selection(
                                doctorId             = doctor.id,
                                slotId               = slot.id,
                                doctorDisplayName    = doctor.displayName,
                                specialtyDisplayName = doctor.specialty.displayName,
                                doctorInitials       = doctor.initials,
                                startsAt             = slot.startsAt,
                                endsAt               = slot.endsAt,
                                locationDisplay      = doctor.locationDisplay(),
                                feeCentavos          = doctor.consultationFee.amountInCentavos,
                            )
                        )
                        navController.navigate(Screen.BookingFlow.createRoute(doctor.id))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                )
            }
        }
    ) { innerPadding ->
        when (val doctorState = uiState.doctor) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LocalMediQColors.current.accent)
                }
            }

            is LoadState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    EmptyState(
                        icon = Icons.Outlined.PersonOff,
                        title = "Doctor not available",
                        description = doctorState.message,
                    )
                }
            }

            is LoadState.Success -> {
                DoctorDetailsContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding),
                    doctor = doctorState.data,
                    uiState = uiState,
                    onDateSelected = { viewModel.onDateSelected(it) },
                    onSlotSelected = { viewModel.onSlotSelected(it) },
                    onPreviousMonth = { viewModel.onPreviousMonth() },
                    onNextMonth = { viewModel.onNextMonth() },
                )
            }
        }
    }
}

@Composable
private fun DoctorDetailsContent(
    modifier: Modifier = Modifier,
    doctor: Doctor,
    uiState: DoctorDetailsUiState,
    onDateSelected: (LocalDate) -> Unit,
    onSlotSelected: (TimeSlot) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val colors = LocalMediQColors.current
    // Read through LocalConfiguration so the day names recompose when the user
    // changes system locale. Locale.getDefault() directly would not observe.
    val locale = LocalConfiguration.current.locales[0]

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // ── Doctor header ────────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediQAvatar(initials = doctor.initials, size = 52.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = doctor.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${doctor.specialty.displayName} · ${doctor.yearsOfExperience} years experience",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "PRC licence ${doctor.licenseNumber} · " +
                        doctor.languages.joinToString(", ") { it.displayName },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
            }
        }

        // ── Fee card ─────────────────────────────────────────────────────
        // The measured pair `MediQAccentLight on MediQLightGreen` (5.89:1) is
        // this card's text on its container, and both tokens are fixed, so the
        // ratio is the same in either mode.
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MediQLightGreen,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Consultation fee",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediQAccentLight,
                    )
                    Text(
                        text = doctor.consultationFee.formatWithCentavos(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediQAccentLight,
                    )
                }
                Text(
                    text = doctor.locationDisplay(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MediQAccentLight,
                )
            }
        }

        // ── Bio ──────────────────────────────────────────────────────────
        // The figure omits this, but `Doctor.bio` is readable nowhere else in
        // the app, so dropping the block would leave a real field unreadable.
        if (doctor.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = doctor.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryText,
                )
            }
        }

        // ── Clinic hours ─────────────────────────────────────────────────
        if (doctor.clinicHours.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ClinicHoursSection(clinicHours = doctor.clinicHours, locale = locale)
        }

        // ── Availability ──────────────────────────────────────────────────
        // Extracted to `SlotPicker` rather than left inline: the appointment
        // reschedule flow needs the same date-strip-and-slot-grid, and two
        // hand-maintained copies of this styling are how they drift. The month
        // stepper is figure 4's; the reschedule picker passes none.
        Spacer(modifier = Modifier.height(24.dp))
        SlotPicker(
            availableDates = uiState.availableDates,
            selectedDate = uiState.selectedDate,
            // `visibleSlots` filters bookable-plus-claimed in the ViewModel,
            // where a test pins it - not here, where nothing would.
            slots = uiState.visibleSlots,
            selectedSlot = uiState.selectedSlot,
            onDateSelected = onDateSelected,
            onSlotSelected = onSlotSelected,
            modifier = Modifier.padding(horizontal = 16.dp),
            monthStepper = MonthStepper(
                label = uiState.month.format(MONTH_YEAR),
                canGoBack = uiState.canGoToPreviousMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
            ),
        )

        // Clearance for the pinned bottom action.
        Spacer(modifier = Modifier.height(80.dp))
    }
}

/**
 * Figure 4's `Clinic hours` block: seven weekday chips, open days in primary
 * text and closed days dimmed, with the times drawn once when every open day
 * shares them.
 *
 * The seed gives each open day two ranges, so a single range cannot be assumed.
 * When the open days do not all share the same ranges, the block falls back to
 * one line per day - the only rendering that cannot print a time beside a day it
 * does not belong to.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClinicHoursSection(
    clinicHours: List<ClinicHours>,
    locale: Locale,
) {
    val colors = LocalMediQColors.current
    val openDays: Set<DayOfWeek> = clinicHours.map { it.dayOfWeek }.toSet()

    // Each day's ranges, ordered as the server sent them.
    val rangesByDay: Map<DayOfWeek, List<String>> = clinicHours
        .groupBy { it.dayOfWeek }
        .mapValues { (_, sessions) -> sessions.map { "${it.opensAt} – ${it.closesAt}" } }

    // The fast path prints the times once, and only when every open day agrees.
    val sharedRanges: List<String>? = rangesByDay.values.distinct().singleOrNull()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Clinic hours",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Monday-first, matching the figure's Mon-Sun strip.
            MONDAY_FIRST.forEach { day ->
                WeekdayChip(
                    label = day.getDisplayName(TextStyle.SHORT, locale),
                    open = day in openDays,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (sharedRanges != null) {
            Text(
                text = sharedRanges.joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryText,
            )
        } else {
            rangesByDay.forEach { (day, ranges) ->
                Text(
                    text = "${day.getDisplayName(TextStyle.SHORT, locale)}  " +
                        ranges.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryText,
                )
            }
        }
    }
}

@Composable
private fun WeekdayChip(label: String, open: Boolean) {
    val colors = LocalMediQColors.current

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            // A closed day is dimmed rather than removed, which is the figure's
            // own convention and is truthful: a day absent from `clinicHours` is
            // a day the doctor does not see patients.
            color = if (open) MaterialTheme.colorScheme.onSurface else colors.secondaryText,
            fontWeight = if (open) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** `Main Building · 3rd Floor · Room 312`, joined in one place. */
private fun Doctor.locationDisplay(): String =
    "${location.building} · ${location.floor} · ${location.room}"

private val MONDAY_FIRST: List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY,
)

private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
