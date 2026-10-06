package com.example.mediq.ui.feature.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.model.toClinicTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A date strip and a time-slot grid, in that order.
 *
 * Stateless and shared, because two screens now need it and only one of them has
 * the appointment context that the reschedule flow starts from. Extracted from
 * `DoctorDetailsScreen`, which had it inline — the alternative was a second
 * copy of the same chip styling, which is how the two drift apart.
 *
 * Both halves take a `LoadState` and render all three cases. The date strip shows
 * its error inline rather than replacing itself with an empty state, because a
 * patient who cannot see this month's dates can still scroll on and act.
 *
 * [slots] should already be filtered to bookable ones by whoever owns it. The
 * filter is deliberately not repeated here: `AppointmentDetailsViewModel` does it
 * so a test can pin it, and doing it again in the composable would mean the UI
 * could show a slot the ViewModel would then refuse to submit.
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
    datesTitle: String = "Available Dates",
    slotsTitle: String = "Available Times",
) {
    val colors = LocalMediQColors.current

    Column(modifier = modifier) {
        Text(
            text = datesTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableDates.data.forEach { available ->
                            FilterChip(
                                selected = available.date == selectedDate,
                                onClick = { onDateSelected(available.date) },
                                label = {
                                    Text(
                                        text = available.date.format(MONTH_DAY),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                // A fixed light pair: white on the brand green is
                                // 6.63:1, and stays so in either mode.
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MediQGreen,
                                    selectedLabelColor = MediQOnBrand,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // A slot list is meaningless until a date is chosen, so the whole section
        // is withheld rather than shown empty — otherwise the patient sees
        // "No open slots" for a date they have not picked.
        if (selectedDate == null) return@Column

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = slotsTitle,
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
                            SlotChip(
                                slot = slot,
                                selected = slot.id == selectedSlot?.id,
                                onClick = { onSlotSelected(slot) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotChip(
    slot: TimeSlot,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalMediQColors.current

    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) MediQGreen else colors.outline),
        color = if (selected) MediQLightGreen else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = slot.startsAt.toClinicTime().toString(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            // Selected is a fixed pair: brand green on the light green chip is
            // 5.89:1.
            color = if (selected) MediQGreen else colors.secondaryText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private val MONTH_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")