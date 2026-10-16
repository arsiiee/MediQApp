package com.example.mediq.ui.feature.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mediq.core.designsystem.component.MediQSlotChip
import com.example.mediq.core.designsystem.component.MediQSlotState
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.core.designsystem.theme.MediQOnBrandMuted
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.SlotStatus
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.model.toClinicTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Figure 4's month stepper, drawn as `‹ November 2026 ›` beside *Choose a date*.
 *
 * Optional on [SlotPicker] because the picker is shared: figure 4's screen owns
 * the month, and the reschedule picker does not. Passing `null` draws no
 * chevrons at all, so the reschedule rendering does not move.
 *
 * There is deliberately no `canGoForward`. [com.example.mediq.ui.feature.doctors.DoctorDetailsViewModel]
 * has no month ceiling - a month the doctor never works comes back as an empty
 * list, which is a truthful answer - so the forward chevron is always live.
 * [onPrevious] is the one that can be dead, on the clinic's own month.
 */
class MonthStepper(
    val label: String,
    val canGoBack: Boolean,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
)

/**
 * A month stepper, a date strip and a time-slot grid, in that order.
 *
 * Stateless and shared, because two screens need it and only one of them has the
 * month context the doctor-details flow starts from. Both halves take a
 * `LoadState` and render all three cases; the date strip shows its error inline
 * rather than replacing itself with an empty state, because a patient who cannot
 * see this month's dates can still scroll on and act.
 *
 * [slots] should already be filtered by whoever owns the data. The filter is
 * deliberately not repeated here: `DoctorDetailsViewModel.visibleSlots` does it
 * so a test can pin it, and doing it again in the composable would mean the UI
 * could show a slot the ViewModel would then refuse to submit. Note that the
 * filter is not *bookable-only* - figure 4 draws `RESERVED` slots greyed under
 * the legend below, so they arrive here as `Claimed`.
 *
 * [slotsTitle] defaults to figure 4's `Available slots · Tue 10 Nov`, built from
 * the selected date. `AppointmentDetailsScreen` overrides it, because it is
 * picking replacement times for an appointment, not choosing a first one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlotPicker(
    availableDates: LoadState<List<AvailableDate>>,
    selectedDate: LocalDate?,
    slots: LoadState<List<TimeSlot>>,
    selectedSlot: TimeSlot?,
    onDateSelected: (LocalDate) -> Unit,
    onSlotSelected: (TimeSlot) -> Unit,
    modifier: Modifier = Modifier,
    monthStepper: MonthStepper? = null,
    datesTitle: String = "Choose a date",
    slotsTitle: String? = null,
) {
    val colors = LocalMediQColors.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = datesTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (monthStepper != null) {
                MonthStepperRow(monthStepper)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when (availableDates) {
            is LoadState.Loading -> CircularProgressIndicator(
                modifier = Modifier.height(24.dp),
                color = colors.accent,
                strokeWidth = 2.dp,
            )

            is LoadState.Error -> Text(
                text = availableDates.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            is LoadState.Success -> {
                if (availableDates.data.isEmpty()) {
                    Text(
                        text = "No available dates this month.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(availableDates.data) { available ->
                            DateCell(
                                available = available,
                                selected = available.date == selectedDate,
                                onClick = { onDateSelected(available.date) },
                            )
                        }
                    }
                    if (availableDates.data.size > VISIBLE_DATE_CELLS) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Slide for more dates",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryText,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // A slot list is meaningless until a date is chosen, so the whole section
        // is withheld rather than shown empty - otherwise the patient sees
        // "No open slots" for a date they have not picked.
        if (selectedDate == null) return@Column

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = slotsTitle ?: "Available slots · ${selectedDate.format(SLOT_HEADING)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (slots) {
            is LoadState.Loading -> CircularProgressIndicator(
                modifier = Modifier.height(24.dp),
                color = colors.accent,
                strokeWidth = 2.dp,
            )

            is LoadState.Error -> Text(
                text = slots.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            is LoadState.Success -> {
                if (slots.data.isEmpty()) {
                    Text(
                        text = "No open slots on this date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        slots.data.forEach { slot ->
                            MediQSlotChip(
                                label = slot.startsAt.toClinicTime().toString(),
                                state = slotState(slot, selectedSlot),
                                onClick = { onSlotSelected(slot) },
                            )
                        }
                    }
                    // Only drawn when it is true of something on screen. A
                    // `BLOCKED` slot is the clinic's own leave, not another
                    // patient, and never reaches here.
                    if (slots.data.any { it.status == SlotStatus.RESERVED }) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Grey = already claimed by another patient",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryText,
                        )
                    }
                }
            }
        }
    }
}

/**
 * `‹ November 2026 ›`. The chevrons are 3:1 non-text graphics, so a disabled one
 * may sit at [LocalMediQColors.outline] (3.45:1 on white) and a live one at
 * `secondaryText`.
 */
@Composable
private fun MonthStepperRow(stepper: MonthStepper) {
    val colors = LocalMediQColors.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperChevron(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            description = "Previous month",
            enabled = stepper.canGoBack,
            onClick = stepper.onPrevious,
        )
        Text(
            text = stepper.label,
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        StepperChevron(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            description = "Next month",
            enabled = true,
            onClick = stepper.onNext,
        )
    }
}

@Composable
private fun StepperChevron(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalMediQColors.current

    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = if (enabled) colors.secondaryText else colors.outline,
        modifier = Modifier
            .size(28.dp)
            .clickable(enabled = enabled, onClick = onClick),
    )
}

/**
 * One date in the strip: `MON` over `9`, 52dp square. Selected fills brand green;
 * the selected cell also carries the figure's `N open` caption underneath.
 */
@Composable
private fun DateCell(
    available: AvailableDate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalMediQColors.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selected) MediQGreen else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .size(52.dp)
                .clickable(onClick = onClick),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = available.date.format(WEEKDAY).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    // Fixed pair on the green: MediQOnBrandMuted is 5.46:1 there.
                    color = if (selected) MediQOnBrandMuted else colors.secondaryText,
                )
                Text(
                    text = available.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MediQOnBrand else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (selected) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${available.openSlotCount} open",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
            )
        } else {
            // Keeps every cell the same height, so the strip does not jump when
            // the selection moves.
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/**
 * The chip state figure 4 draws: selected wins, then claimed, then available.
 * `Claimed` and `Selected` are not clickable, so a reserved slot cannot be
 * chosen by tapping it.
 */
private fun slotState(slot: TimeSlot, selectedSlot: TimeSlot?): MediQSlotState = when {
    slot.id == selectedSlot?.id -> MediQSlotState.Selected
    slot.status == SlotStatus.RESERVED -> MediQSlotState.Claimed
    else -> MediQSlotState.Available
}

private val WEEKDAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")
private val SLOT_HEADING: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

/** How many 52dp cells fit the seeded phone width; fewer than this needs no hint. */
private const val VISIBLE_DATE_CELLS = 5
