package com.example.mediq.ui.screens.booking

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mediq.ui.components.MediQNotice
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorContainer
import com.example.mediq.ui.theme.MediQWarning
import com.example.mediq.ui.theme.MediQWarningContainer

/**
 * What went wrong with the submission, if anything.
 *
 * A slot that went to someone else gets the way out in the notice itself —
 * there is nothing to retry, so the notice says so instead of leaving a dead
 * error up.
 */
@Composable
internal fun SubmissionNotice(
    submission: BookingSubmission,
    actions: BookingActions,
    modifier: Modifier = Modifier
) {
    when (submission) {
        is BookingSubmission.SlotTaken -> MediQNotice(
            icon = Icons.Outlined.CalendarMonth,
            message = submission.message,
            container = MediQWarningContainer,
            content = MediQWarning,
            actionLabel = "Choose another slot",
            onAction = actions.onChooseAnotherSlot,
            modifier = modifier
        )

        is BookingSubmission.Error -> MediQNotice(
            icon = Icons.Outlined.CloudOff,
            message = submission.message,
            container = MediQErrorContainer,
            content = MediQError,
            actionLabel = null,
            onAction = null,
            modifier = modifier
        )

        BookingSubmission.Idle, BookingSubmission.Submitting -> Unit
    }
}
