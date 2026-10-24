package com.example.mediq.ui.screens.home

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
import androidx.compose.material.icons.filled.CloudOff
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
 * The non-content home states: loading and error.
 *
 * Both are announced to screen readers and both keep the screen's job visible:
 * the skeleton mirrors the loaded layout, the error offers the retry.
 */

/** Skeleton that mirrors the loaded layout, so nothing jumps when data arrives. */
@Composable
internal fun HomeLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
            .padding(horizontal = MediQSpacing.gutter)
            .semantics { contentDescription = "Loading your home screen" }
    ) {
        Spacer(Modifier.height(MediQSpacing.xxl))

        SkeletonBar(widthFraction = 0.45f, height = MediQSpacing.xl)

        Spacer(Modifier.height(MediQSpacing.lg))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MediQBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = MediQElevation.none)
        ) {
            Column(modifier = Modifier.padding(MediQSpacing.lg)) {
                SkeletonBar(widthFraction = 0.55f, height = MediQSize.skeletonBarSm)
                Spacer(Modifier.height(MediQSpacing.md))
                SkeletonBar(widthFraction = 0.35f, height = MediQSize.skeletonBarLg)
                Spacer(Modifier.height(MediQSpacing.sm))
                SkeletonBar(widthFraction = 0.7f, height = MediQSize.skeletonBarSm)
                Spacer(Modifier.height(MediQSpacing.lg))
                SkeletonBar(widthFraction = 0.5f, height = MediQSize.skeletonBarSm)
            }
        }

        Spacer(Modifier.height(MediQSpacing.lg))

        SkeletonBar(widthFraction = 0.6f, height = MediQSize.minTouchTarget)
    }
}

/** A single skeleton bar. Decorative, so it is hidden from screen readers. */
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
internal fun HomeErrorState(
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
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = MediQTextSecondary,
            modifier = Modifier.size(MediQSize.control)
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(
            text = "We couldn't load your home screen",
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
