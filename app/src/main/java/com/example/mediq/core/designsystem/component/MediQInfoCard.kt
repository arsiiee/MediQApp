package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mediq.core.designsystem.theme.MediQError
import com.example.mediq.core.designsystem.theme.MediQErrorContainer
import com.example.mediq.core.designsystem.theme.MediQInfoContainer
import com.example.mediq.core.designsystem.theme.MediQInfoContent
import com.example.mediq.core.designsystem.theme.MediQWarningContainer
import com.example.mediq.core.designsystem.theme.MediQWarningOnContainer

/** Which status container a [MediQInfoCard] sits on, if any. */
enum class MediQInfoCardTint { None, Warning, Error, Info }

/**
 * A bordered card. The container is the `surface` role or a status container -
 * never a hardcoded white, which is invisible against the light page (1.24:1)
 * and is the exact literal the contrast gate exists to keep out.
 *
 * A tinted card draws its border *and* its content in the same status role, so
 * the pair the gate measures is the pair actually on screen: `MediQError` on
 * `MediQErrorContainer` is 6.43:1, `MediQWarningOnContainer` on
 * `MediQWarningContainer` 5.96:1, `MediQInfoContent` on `MediQInfoContainer`
 * 5.03:1. An untinted card keeps the default `onSurface` text and a
 * `outlineVariant` hairline, which is decorative and carries no contrast floor.
 *
 * Setting the content colour here rather than at each call site is what stops a
 * card's text falling back to `onSurface` and leaving the measured pair unused.
 */
@Composable
fun MediQInfoCard(
    modifier: Modifier = Modifier,
    tint: MediQInfoCardTint = MediQInfoCardTint.None,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = when (tint) {
        MediQInfoCardTint.None    -> MaterialTheme.colorScheme.surface
        MediQInfoCardTint.Warning -> MediQWarningContainer
        MediQInfoCardTint.Error   -> MediQErrorContainer
        MediQInfoCardTint.Info    -> MediQInfoContainer
    }
    val border: Color = when (tint) {
        MediQInfoCardTint.None    -> MaterialTheme.colorScheme.outlineVariant
        MediQInfoCardTint.Warning -> MediQWarningOnContainer
        MediQInfoCardTint.Error   -> MediQError
        MediQInfoCardTint.Info    -> MediQInfoContent
    }
    val contentColor: Color = when (tint) {
        MediQInfoCardTint.None    -> MaterialTheme.colorScheme.onSurface
        MediQInfoCardTint.Warning -> MediQWarningOnContainer
        MediQInfoCardTint.Error   -> MediQError
        MediQInfoCardTint.Info    -> MediQInfoContent
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = BorderStroke(1.dp, border),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content,
            )
        }
    }
}
