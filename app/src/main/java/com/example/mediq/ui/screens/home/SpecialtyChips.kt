package com.example.mediq.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary

/**
 * Specialty filter chips.
 *
 * Each chip is a real control: it has a 48dp touch target and an
 * `onClickLabel` so a screen reader announces where the tap leads.
 */
@Composable
fun SpecialtyChips(
    specialties: List<String>,
    onSpecialtyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (specialties.isEmpty()) return

    Column(modifier = modifier) {

        SectionTitle(text = "Browse by specialty")

        Spacer(Modifier.height(MediQSpacing.md))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MediQSpacing.sm),
            contentPadding = PaddingValues(horizontal = MediQSpacing.gutter)
        ) {
            items(items = specialties, key = { it }) { specialty ->
                SpecialtyChip(
                    specialty = specialty,
                    onClick = { onSpecialtyClick(specialty) }
                )
            }
        }
    }
}

@Composable
private fun SpecialtyChip(
    specialty: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(MediQRadius.pill),
        color = MediQBackground,
        border = BorderStroke(MediQSize.hairline, MediQBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(MediQRadius.pill))
            .clickable(onClickLabel = "Browse $specialty doctors", onClick = onClick)
            .heightIn(min = MediQSize.minTouchTarget)
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = MediQSpacing.md,
                vertical = MediQSpacing.sm
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = specialty,
                style = MaterialTheme.typography.labelMedium,
                color = MediQTextPrimary
            )
        }
    }
}
