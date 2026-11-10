package com.example.mediq.ui.screens.doctors

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
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 * The non-result doctors states: loading, empty and error.
 *
 * The two empty cases are distinct and say different things: an empty catalogue
 * ("nothing to show") is not the same as filters that match nothing.
 */

/** Skeleton that mirrors a stack of result cards. */
@Composable
internal fun DoctorsLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
            .padding(MediQSpacing.gutter)
            .semantics { contentDescription = "Loading doctors" },
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.md)
    ) {
        Spacer(Modifier.height(MediQSpacing.xs))

        SkeletonBar(widthFraction = 1f, height = MediQSize.minTouchTarget)

        repeat(4) {
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
                    Spacer(Modifier.height(MediQSpacing.sm))
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

/** Filters match nothing. Offers the way out rather than a dead end. */
@Composable
internal fun DoctorsNoMatchesState(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.SearchOff,
        title = "No doctors match these filters",
        message = "Try a different spelling, or widen the specialty and location filters.",
        actionLabel = "Clear filters",
        onAction = onClearFilters,
        modifier = modifier
    )
}

/** The clinic has published no doctors at all. */
@Composable
internal fun DoctorsEmptyState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.SearchOff,
        title = "No doctors available yet",
        message = "Doctors appear here as soon as the clinic publishes their schedules.",
        actionLabel = null,
        onAction = null,
        modifier = modifier
    )
}

/** Recoverable failure: says what happened and offers the retry. */
@Composable
internal fun DoctorsErrorState(
    message: String,
    onRetry: () -> Unit
) {
    EmptyState(
        icon = Icons.Outlined.CloudOff,
        title = "We couldn't load the doctor list",
        message = message,
        actionLabel = "Try again",
        onAction = onRetry,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MediQSpacing.gutter,
                vertical = MediQSpacing.xxxl
            ),
        horizontalAlignment = Alignment.CenterHorizontally
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

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(MediQSpacing.lg))

            TextButton(
                onClick = onAction,
                shape = RoundedCornerShape(MediQRadius.md),
                modifier = Modifier.height(MediQSize.minTouchTarget)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

