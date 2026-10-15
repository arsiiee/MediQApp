package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQLightGreen

/**
 * A doctor's or patient's initials in a tinted circle.
 *
 * No photograph: the backend has no image field, and a stock placeholder
 * portrait would be invented data. [MediQGreen] on [MediQLightGreen] is 5.89:1
 * in both modes, so one tint serves light and dark.
 */
@Composable
fun MediQAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MediQLightGreen, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = initials.uppercase(),
            style      = MaterialTheme.typography.titleMedium,
            color      = MediQGreen,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
