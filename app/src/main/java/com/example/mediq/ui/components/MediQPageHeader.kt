package com.example.mediq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mediq.ui.theme.MediQDarkGreen
import com.example.mediq.ui.theme.MediQDivider
import com.example.mediq.ui.theme.MediQHeaderControl
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * Single source of truth for every subpage header.
 *
 * Renders [title] at the shared page-title styling (headlineMedium, dark green,
 * gutter padding) so every subpage looks consistent. Detail screens pass a
 * non-null [onBack] to get the 48 dp back-arrow touch target; top-level tab
 * screens pass null and drop the arrow. An optional [subtitle] and [showDivider]
 * keep the list-screen chrome where screens already had it. The trailing
 * [actions] slot hosts detail-screen controls.
 *
 * Header controls (back arrow, action icons) use [MediQHeaderControl] per
 * design-contract §5 — never a second brand green on a screen.
 *
 * [onBack] is nullable: a terminal screen such as a booking result drops the
 * control rather than showing a dead one. The drawn icon stays small while the
 * tap target stays at [MediQSize.minTouchTarget] (48 dp).
 */
@Composable
fun MediQPageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = MediQSpacing.lg,
                start = MediQSpacing.gutter,
                end = MediQSpacing.gutter,
                bottom = MediQSpacing.sm
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        tint = MediQHeaderControl,
                        modifier = Modifier.size(MediQSize.iconLarge)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = -0.5.sp
                ),
                color = MediQDarkGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = MediQSpacing.xs)
            )

            actions()
        }

        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary,
                modifier = Modifier.padding(top = MediQSpacing.xxs)
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(top = MediQSpacing.sm)
                    .background(MediQDivider)
            )
        }
    }
}
