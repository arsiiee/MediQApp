package com.example.mediq.ui.screens.doctordetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * One day in the strip.
 *
 * The hit area is at least [MediQSize.minTouchTarget] tall and the selected
 * state is announced, not just painted green.
 */
@Composable
internal fun DateCard(
    date: DateOption,
    openSlots: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (isSelected) MediQGreen else MediQBackground
    val primaryText = if (isSelected) MediQOnBrand else MediQTextPrimary
    val secondaryText = if (isSelected) MediQOnBrand else MediQTextSecondary
    val accentText = if (isSelected) MediQOnBrand else MediQGreen

    Column(
        modifier = modifier
            .heightIn(min = MediQSize.minTouchTarget)
            .clip(MaterialTheme.shapes.large)
            .background(background)
            .then(
                if (isSelected) {
                    Modifier
                } else {
                    Modifier.border(
                        width = MediQSize.hairline,
                        color = MediQBorder,
                        shape = MaterialTheme.shapes.large
                    )
                }
            )
            .semantics { selected = isSelected }
            .clickable(
                role = Role.Button,
                onClickLabel = "Show slots for ${date.day} ${date.date}",
                onClick = onSelect
            )
            .padding(
                horizontal = MediQSpacing.xs,
                vertical = MediQSpacing.sm
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.day,
            style = MaterialTheme.typography.labelSmall,
            color = secondaryText
        )

        Text(
            text = date.date.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = primaryText
        )

        Text(
            text = "$openSlots open",
            style = MaterialTheme.typography.labelSmall,
            color = accentText
        )
    }
}
