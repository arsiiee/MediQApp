package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mediq.core.designsystem.theme.MediQError
import com.example.mediq.core.designsystem.theme.MediQErrorContainer
import com.example.mediq.core.designsystem.theme.MediQInfoContainer
import com.example.mediq.core.designsystem.theme.MediQInfoContent
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.core.designsystem.theme.MediQNeutralContainer
import com.example.mediq.core.designsystem.theme.MediQNeutralContent
import com.example.mediq.core.designsystem.theme.MediQSuccess
import com.example.mediq.core.designsystem.theme.MediQWarningContainer
import com.example.mediq.core.designsystem.theme.MediQWarningOnContainer
import com.example.mediq.domain.model.AppointmentStatus

/**
 * The one appointment-status badge, for `AppointmentsScreen` and
 * `AppointmentDetailsScreen`.
 *
 * It lives here rather than in a screen so the two cannot drift into showing the
 * same status in two different colours, and so the mapping is measured once:
 * `check-contrast.ps1` parses the six `... to ...` lines below out of this file.
 *
 * Both halves of every pair are fixed, so these chips keep the same measured
 * contrast in either mode. That is deliberate, and `CONSTRAINTS.md` carries it
 * as known debt. The amber was #F9A825 on #FFF8E1, which is 1.85:1 - unreadable
 * at 10sp.
 */
@Composable
fun MediQStatusChip(status: AppointmentStatus) {
    val (bg, fg) = when (status) {
        AppointmentStatus.CONFIRMED           -> MediQLightGreen to MediQSuccess
        AppointmentStatus.PENDING_CONFIRMATION -> MediQWarningContainer to MediQWarningOnContainer
        AppointmentStatus.COMPLETED           -> MediQInfoContainer to MediQInfoContent
        AppointmentStatus.CANCELLED           -> MediQErrorContainer to MediQError
        AppointmentStatus.DECLINED            -> MediQErrorContainer to MediQError
        // Grey, because the meaning is not known. Reusing a status colour here
        // would imply a conclusion the app has not reached.
        AppointmentStatus.UNKNOWN             -> MediQNeutralContainer to MediQNeutralContent
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(
            text     = status.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = fg,
            fontSize = 10.sp,
        )
    }
}
