package com.example.mediq.ui.screens.appointmentdetails

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
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.example.mediq.ui.theme.MediQNeutralContainer
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/*
 * The non-content detail states: loading and error.
 *
 * The header sits above both in AppointmentDetailsContent, so each state only
 * fills what is left.
 */

/** Skeleton that mirrors the loaded card, the map and the actions. */
@Composable
internal fun AppointmentDetailsLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
            .padding(MediQSpacing.gutter)
            .semantics { contentDescription = "Loading this appointment" },
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.md)
    ) {
        Spacer(Modifier.height(MediQSpacing.xs))

        SkeletonBar(widthFraction = 1f, height = MediQSpacing.xxxl)
        SkeletonBar(widthFraction = 1f, height = MediQSpacing.lg)
        SkeletonBar(widthFraction = 1f, height = MediQSize.mapPreview)
        SkeletonBar(widthFraction = 1f, height = MediQSize.minTouchTarget)
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

/** Recoverable failure: says what happened and offers the retry. */
@Composable
internal fun AppointmentDetailsErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
            .padding(MediQSpacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MediQTextSecondary,
            modifier = Modifier.size(MediQSize.control)
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(
            text = "We couldn't open this appointment",
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
