package com.example.mediq.ui.screens.doctordetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQNeutral
import com.example.mediq.ui.theme.MediQNeutralContainer
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSlotAvailableBorder
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQSuccess
import com.example.mediq.ui.theme.MediQSuccessContainer
import com.example.mediq.ui.theme.MediQTextSecondary

/*
 * The slot grid and its legend.
 *
 * Every control here is at least [MediQSize.minTouchTarget] on its hit area and
 * announces its own state — selection through its `selected` semantics, slot
 * availability through its content description — so colour is never the only
 * signal.
 */

/** A row of the slot grid. Short rows keep their alignment with empty weight. */
@Composable
internal fun TimeSlotRow(
    slots: List<String>,
    statuses: Map<String, TimeSlotStatus>,
    selectedTime: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MediQSpacing.sm)
    ) {
        slots.forEach { time ->
            TimeSlotButton(
                time = time,
                status = statuses[time] ?: TimeSlotStatus.RESERVED,
                isSelected = selectedTime == time,
                onSelect = onSelect,
                modifier = Modifier.weight(1f)
            )
        }

        // Keeps the final row aligned when it holds fewer than three slots.
        repeat(3 - slots.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * One slot.
 *
 * Reserved and blocked slots are drawn but not selectable, and their content
 * description says which is which — a screen reader gets "11:30 AM, reserved",
 * not just the time.
 */
@Composable
internal fun TimeSlotButton(
    time: String,
    status: TimeSlotStatus,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val available = status == TimeSlotStatus.AVAILABLE
    val background = when {
        isSelected -> MediQGreen
        available -> MediQSuccessContainer
        status == TimeSlotStatus.BLOCKED -> MediQErrorContainer
        else -> MediQNeutralContainer
    }
    val content = when {
        isSelected -> MediQOnBrand
        available -> MediQGreen
        status == TimeSlotStatus.BLOCKED -> MediQError
        else -> MediQTextSecondary
    }
    val statusLabel = when (status) {
        TimeSlotStatus.AVAILABLE -> "available"
        TimeSlotStatus.RESERVED -> "reserved"
        TimeSlotStatus.BLOCKED -> "blocked"
    }

    Box(
        modifier = modifier
            .heightIn(min = MediQSize.minTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .then(
                if (available && !isSelected) {
                    Modifier.border(
                        width = MediQSize.hairline,
                        color = MediQSlotAvailableBorder,
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = "$time, $statusLabel"
                selected = isSelected
            }
            .clickable(
                enabled = available,
                role = Role.Button,
                onClickLabel = "Select $time",
                onClick = { onSelect(time) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(vertical = MediQSpacing.xs)
        )
    }
}

/** Legend entry: the colour swatch sits next to its word, never instead of it. */
@Composable
internal fun LegendEntry(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(MediQSpacing.sm)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(MediQSpacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MediQTextSecondary
        )
    }
}
