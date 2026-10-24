package com.example.mediq.ui.screens.doctordetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQDisabledContainer
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQNeutral
import com.example.mediq.ui.theme.MediQNeutralContainer
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQSuccess
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * "Choose a time slot": the day strip, the grid, the legend and the way out.
 *
 * Picking a different day clears the chosen time, so [onContinue] can never
 * report a slot from a day that is no longer on screen.
 */
@Composable
internal fun DoctorSlotPicker(
    schedule: List<DateSchedule>,
    onContinue: (dateIndex: Int, time: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // The first day with something to book, so the grid opens on real options.
    var selectedDateIndex by remember(schedule) {
        mutableStateOf(
            schedule.indexOfFirst { it.openSlots > 0 }.coerceAtLeast(0)
        )
    }

    // Keyed to the day: changing the day drops the chosen time with it.
    var selectedTime by remember(selectedDateIndex) {
        mutableStateOf<String?>(null)
    }

    val selectedDay = schedule[selectedDateIndex]

    Column(
        modifier = modifier.padding(
            horizontal = MediQSpacing.gutter,
            vertical = MediQSpacing.lg
        )
    ) {
        Text(
            text = "Choose a time slot",
            style = MaterialTheme.typography.titleSmall,
            color = MediQTextPrimary
        )

        Spacer(Modifier.height(MediQSpacing.xs))

        Text(
            text = "Clinic hours are 9:00 AM–5:00 PM. Reserved and blocked " +
                    "slots cannot be selected.",
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextSecondary
        )

        Spacer(Modifier.height(MediQSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MediQSpacing.sm)
        ) {
            schedule.forEachIndexed { index, day ->
                DateCard(
                    date = day.option,
                    openSlots = day.openSlots,
                    isSelected = index == selectedDateIndex,
                    onSelect = { selectedDateIndex = index },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(MediQSpacing.md))

        if (selectedDay.openSlots == 0) {
            NoOpenSlotsOnThisDay()
            Spacer(Modifier.height(MediQSpacing.sm))
        }

        AllTimeSlots.chunked(3).forEach { rowSlots ->
            TimeSlotRow(
                slots = rowSlots,
                statuses = selectedDay.statuses,
                selectedTime = selectedTime,
                onSelect = { selectedTime = it }
            )
            Spacer(Modifier.height(MediQSpacing.xs))
        }

        Spacer(Modifier.height(MediQSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MediQSpacing.lg)
        ) {
            LegendEntry(color = MediQSuccess, label = "Available")
            LegendEntry(color = MediQNeutral, label = "Reserved")
            LegendEntry(color = MediQError, label = "Blocked")
        }

        Spacer(Modifier.height(MediQSpacing.lg))

        ContinueButton(
            time = selectedTime,
            enabled = selectedTime != null,
            onClick = {
                selectedTime?.let { time ->
                    onContinue(selectedDateIndex, time)
                }
            }
        )
    }
}

/** A day with nothing left to book says so instead of showing a dead grid. */
@Composable
private fun NoOpenSlotsOnThisDay(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MediQNeutralContainer
    ) {
        Text(
            text = "No open slots on this day. Pick another day above.",
            style = MaterialTheme.typography.bodySmall,
            color = MediQTextSecondary,
            modifier = Modifier.padding(MediQSpacing.md)
        )
    }
}

/**
 * The screen's primary action.
 *
 * It says what it will do: the chosen time while there is one, otherwise why
 * it cannot be pressed yet. The disabled state is the one place the button
 * leaves [MediQGreen].
 */
@Composable
private fun ContinueButton(
    time: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MediQSize.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MediQGreen,
            contentColor = MediQOnBrand,
            disabledContainerColor = MediQDisabledContainer,
            disabledContentColor = MediQOnBrand
        )
    ) {
        Text(
            text = time?.let { "Continue · $it" } ?: "Select a slot to continue",
            style = MaterialTheme.typography.labelLarge
        )
    }
}
