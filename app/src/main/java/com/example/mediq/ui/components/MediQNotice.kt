package com.example.mediq.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary

/**
 * An in-body notice: one icon, one sentence, and an action when the reader has
 * something useful to do about it.
 *
 * The tint pair is passed in so one shape covers error, warning and neutral —
 * the colour never carries the meaning on its own, the sentence and the icon
 * do. A notice with an action puts that action inside itself rather than
 * leaving a dead error standing.
 */
@Composable
fun MediQNotice(
    icon: ImageVector,
    message: String,
    container: Color,
    content: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container
    ) {
        Row(
            modifier = Modifier.padding(MediQSpacing.md),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier
                    .size(MediQSize.icon)
                    .padding(top = MediQSpacing.xxs)
            )

            Spacer(Modifier.width(MediQSpacing.sm))

            Column {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediQTextPrimary
                )

                if (actionLabel != null && onAction != null) {
                    TextButton(
                        onClick = onAction,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.heightIn(min = MediQSize.minTouchTarget)
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = content
                        )
                    }
                }
            }
        }
    }
}
