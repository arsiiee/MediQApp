package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediq.core.designsystem.theme.MediQErrorContainer
import com.example.mediq.core.designsystem.theme.MediQInfoContainer
import com.example.mediq.core.designsystem.theme.MediQWarningContainer

/** Which status container a [MediQInfoCard] sits on, if any. */
enum class MediQInfoCardTint { None, Warning, Error, Info }

/**
 * A bordered card. The container is the `surface` role or a status container -
 * never a hardcoded white, which is invisible against the light page (1.24:1)
 * and is the exact literal the contrast gate exists to keep out.
 *
 * The border is `outlineVariant`: decorative, so it carries no contrast floor on
 * its own. Reach for a status tint and the card's own text roles hold the
 * measured ratio.
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}
