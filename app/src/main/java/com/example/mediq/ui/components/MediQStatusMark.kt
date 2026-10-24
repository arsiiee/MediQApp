package com.example.mediq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mediq.ui.theme.MediQSize

/**
 * The round mark at the centre of a terminal state: booking result, account
 * created, and anything else that only has to say "this happened".
 *
 * One component, because a success mark drawn three ways is how the states of
 * one app start to look like three apps. The tint pair is passed in so the same
 * shape works for success, warning and neutral.
 */
@Composable
fun MediQStatusMark(
    icon: ImageVector,
    container: Color,
    tint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(MediQSize.hero)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.fillMaxWidth(0.5f)
        )
    }
}
