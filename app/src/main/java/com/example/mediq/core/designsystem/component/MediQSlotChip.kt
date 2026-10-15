package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand

/** The three states a bookable time can be drawn in. */
enum class MediQSlotState { Available, Selected, Claimed }

/**
 * One bookable time. [MediQSlotState.Claimed] is deliberately not clickable: a
 * claimed slot cannot be selected, so it must not offer a touch target either.
 *
 * The pairs are the accent on `surface` for bookable, [MediQOnBrand] on
 * [MediQGreen] for selected, and `LocalMediQColors.current.secondaryText` on
 * `claimedSurface` for claimed - the last is measured by `check-contrast.ps1` as
 * the token pair `MediQTextSecondary on MediQClaimedSurface`.
 */
@Composable
fun MediQSlotChip(
    label: String,
    state: MediQSlotState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalMediQColors.current
    val container = when (state) {
        MediQSlotState.Available -> MaterialTheme.colorScheme.surface
        MediQSlotState.Selected  -> MediQGreen
        MediQSlotState.Claimed   -> colors.claimedSurface
    }
    val labelColor = when (state) {
        MediQSlotState.Available -> colors.accent
        MediQSlotState.Selected  -> MediQOnBrand
        MediQSlotState.Claimed   -> colors.secondaryText
    }
    val border = when (state) {
        MediQSlotState.Available -> BorderStroke(1.dp, colors.outline)
        MediQSlotState.Selected  -> null
        MediQSlotState.Claimed   -> null
    }
    val action = if (state == MediQSlotState.Available) onClick else null
    Surface(
        modifier = if (action != null) modifier.clickable(onClick = action) else modifier,
        shape = RoundedCornerShape(8.dp),
        color = container,
        border = border,
    ) {
        Text(
            text      = label,
            modifier  = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style     = MaterialTheme.typography.labelMedium,
            color     = labelColor,
            textAlign = TextAlign.Center,
        )
    }
}
