package com.example.mediq.ui.screens.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQElevation
import com.example.mediq.ui.theme.MediQNeutralContainer
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/*
 * The non-list appointments states: loading, empty and error.
 *
 * The empty message depends on the tab, because "no visits coming up" and "no
 * visits so far" are different facts and the patient should be told which one
 * they are looking at.
 */

/** Skeleton that mirrors the tab row and a stack of booking cards. */
@Composable
internal fun AppointmentsLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
            .padding(MediQSpacing.gutter)
            .semantics { contentDescription = "Loading your appointments" },
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.md)
    ) {
        Spacer(Modifier.height(MediQSpacing.xs))

        SkeletonBar(widthFraction = 1f, height = MediQSize.minTouchTarget)

        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MediQBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = MediQElevation.none)
            ) {
                Column(modifier = Modifier.padding(MediQSpacing.lg)) {
                    SkeletonBar(widthFraction = 0.6f, height = MediQSpacing.lg)
                    Spacer(Modifier.height(MediQSpacing.sm))
                    SkeletonBar(widthFraction = 0.4f, height = MediQSpacing.md)
                    Spacer(Modifier.height(MediQSpacing.md))
                    SkeletonBar(widthFraction = 0.8f, height = MediQSpacing.md)
                }
            }
        }
    }
}

@Composable
private fun SkeletonBar(widthFraction: Float, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(MaterialTheme.shapes.small)
            .background(MediQNeutralContainer)
            .clearAndSetSemantics { }
    )
}

/** This tab has no bookings. Says which kind of nothing it is. */
@Composable
internal fun AppointmentsEmptyState(
    tab: AppointmentsTab,
    modifier: Modifier = Modifier
) {
    val (title, message) = when (tab) {
        AppointmentsTab.Upcoming -> "No visits coming up" to
                "Bookings you make show up here, with the day, time and clinic."

        AppointmentsTab.History -> "No past visits yet" to
                "Once a visit is done or cancelled, it moves here for your records."
    }

    AppointmentsMessage(
        icon = Icons.Outlined.CalendarMonth,
        title = title,
        message = message,
        modifier = modifier
    )
}

/** Recoverable failure: says what happened and offers the retry. */
@Composable
internal fun AppointmentsErrorState(
    message: String,
    onRetry: () -> Unit
) {
    AppointmentsMessage(
        icon = Icons.Outlined.CloudOff,
        title = "We couldn't load your appointments",
        message = message,
        onRetry = onRetry,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun AppointmentsMessage(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MediQSpacing.gutter,
                vertical = MediQSpacing.xxxl
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MediQTextSecondary,
            modifier = Modifier.size(MediQSize.control)
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MediQTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(MediQSpacing.sm))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MediQTextSecondary,
            textAlign = TextAlign.Center
        )

        if (onRetry != null) {
            Spacer(Modifier.height(MediQSpacing.xl))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(MediQRadius.md),
                modifier = Modifier.height(MediQSize.minTouchTarget)
            ) {
                Text(
                    text = "Try again",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
