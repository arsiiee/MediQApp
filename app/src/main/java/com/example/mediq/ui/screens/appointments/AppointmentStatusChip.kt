package com.example.mediq.ui.screens.appointments

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorChip
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQSuccessContainer
import com.example.mediq.ui.theme.MediQWarning
import com.example.mediq.ui.theme.MediQWarningContainer

/**
 * One booking's status.
 *
 * The chip always carries the status text, so colour is reinforcement, not the
 * only signal.
 */
@Composable
internal fun AppointmentStatusChip(
    status: AppointmentStatus,
    modifier: Modifier = Modifier
) {
    val tone = status.tone()

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = tone.container
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = tone.content,
            modifier = Modifier.padding(
                horizontal = MediQSpacing.sm,
                vertical = MediQSpacing.xs
            )
        )
    }
}

private data class StatusTone(val container: Color, val content: Color)

private fun AppointmentStatus.tone(): StatusTone = when (this) {
    AppointmentStatus.Confirmed,
    AppointmentStatus.Completed -> StatusTone(MediQSuccessContainer, MediQGreen)

    AppointmentStatus.AwaitingConfirmation -> StatusTone(MediQWarningContainer, MediQWarning)

    AppointmentStatus.Cancelled -> StatusTone(MediQErrorChip, MediQError)
}
