package com.example.mediq.ui.screens.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQNeutralContainer
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * The Upcoming / History switch.
 *
 * Each tab is at least [MediQSize.minTouchTarget] tall and announces whether it
 * is selected, so the state is not carried by the green alone.
 */
@Composable
internal fun AppointmentTabs(
    selected: AppointmentsTab,
    onSelect: (AppointmentsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MediQScaffoldBackground)
            .padding(
                horizontal = MediQSpacing.gutter,
                vertical = MediQSpacing.sm
            ),
        horizontalArrangement = Arrangement.spacedBy(MediQSpacing.sm)
    ) {
        AppointmentsTab.entries.forEach { tab ->
            AppointmentTab(
                tab = tab,
                isSelected = tab == selected,
                onSelect = { onSelect(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppointmentTab(
    tab: AppointmentsTab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = MediQSize.minTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) MediQGreen else MediQNeutralContainer)
            .semantics { selected = isSelected }
            .clickable(
                role = Role.Tab,
                onClickLabel = "Show ${tab.label.lowercase()} appointments",
                onClick = onSelect
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MediQOnBrand else MediQTextSecondary
        )
    }
}
