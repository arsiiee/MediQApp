package com.example.mediq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing

/**
 * Brand-green header for detail screens: back control, title, trailing actions.
 *
 * The back control is a [MediQSize.minTouchTarget] target around a
 * [MediQSize.iconLarge] icon, so it stays reachable while the drawn icon stays
 * small. Everything on the green uses [MediQOnBrand] — never a second green.
 *
 * [onBack] is nullable: a terminal screen such as a booking result has no back
 * action and drops the control rather than showing a dead one.
 *
 * [height] is an optional fixed height for the bar. When provided it replaces
 * the default vertical padding (used by messages and appointments, fixed at 80dp).
 *
 * The trailing [actions] slot lets a screen add its own controls without this
 * header growing a new parameter it does not need yet.
 */
@Composable
fun MediQBackHeader(
    title: String,
    onBack: (() -> Unit)?,
    height: Dp? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MediQGreen)
            .let { m ->
                if (height != null) m.height(height) else m.padding(vertical = MediQSpacing.sm)
            }
            .padding(horizontal = MediQSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(MediQSize.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MediQOnBrand,
                    modifier = Modifier.size(MediQSize.iconLarge)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MediQOnBrand,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = MediQSpacing.xs)
        )

        actions()
    }
}
