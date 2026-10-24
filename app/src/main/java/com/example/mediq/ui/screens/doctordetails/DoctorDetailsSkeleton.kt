package com.example.mediq.ui.screens.doctordetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQNeutralContainer
import com.example.mediq.ui.theme.MediQScaffoldBackground
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing

/**
 * The loading skeleton: profile block, day strip, slot grid, primary action.
 *
 * It mirrors the loaded layout so nothing jumps when the data arrives, and it
 * is announced as a whole rather than bar by bar.
 */
@Composable
internal fun DoctorDetailsLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQScaffoldBackground)
            .padding(MediQSpacing.gutter)
            .semantics { contentDescription = "Loading doctor details" },
        verticalArrangement = Arrangement.spacedBy(MediQSpacing.md)
    ) {
        // Profile block: avatar, name, two figures.
        SkeletonBar(widthFraction = 0.55f, height = MediQSpacing.xl)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MediQSpacing.md)
        ) {
            SkeletonBox(modifier = Modifier.weight(1f))
            SkeletonBox(modifier = Modifier.weight(1f))
        }

        SkeletonBar(widthFraction = 0.9f, height = MediQSpacing.md)
        SkeletonBar(widthFraction = 0.75f, height = MediQSpacing.md)

        Spacer(Modifier.height(MediQSpacing.xs))

        SkeletonBar(widthFraction = 0.4f, height = MediQSpacing.lg)

        // The day strip.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MediQSpacing.sm)
        ) {
            repeat(5) {
                SkeletonBox(modifier = Modifier.weight(1f))
            }
        }

        // Two rows of the slot grid.
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MediQSpacing.sm)
            ) {
                repeat(3) {
                    SkeletonBox(modifier = Modifier.weight(1f))
                }
            }
        }

        SkeletonBox(modifier = Modifier.fillMaxWidth())
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

/** A single skeleton block: a control with no content in it yet. */
@Composable
private fun SkeletonBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(MediQSize.minTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(MediQNeutralContainer)
            .clearAndSetSemantics { }
    )
}
