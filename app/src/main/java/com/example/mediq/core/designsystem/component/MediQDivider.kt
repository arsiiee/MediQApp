package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The hairline between rows and under app bars: 1dp of `outlineVariant`.
 *
 * A divider is decorative (WCAG 1.4.11 exempts it), so it is drawn with the
 * subtle role rather than the 3:1 `MediQOutline` a control's border needs.
 */
@Composable
fun MediQDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
