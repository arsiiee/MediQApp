package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing

/** How many steps the registration flow has. */
internal const val REGISTRATION_STEPS = 3

/** What every registration step calls itself in its header. */
internal const val REGISTRATION_TITLE = "Create your account"

/**
 * How far through registration the patient is.
 *
 * [REGISTRATION_STEPS] bars, filled up to [currentStep]. The row carries one
 * description — "Step 2 of 3" — because coloured rectangles say nothing to a
 * screen reader on their own, and the count is the part that matters.
 */
@Composable
internal fun RegistrationProgress(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Step $currentStep of $REGISTRATION_STEPS"
            },
        horizontalArrangement = Arrangement.spacedBy(MediQSpacing.xs)
    ) {
        repeat(REGISTRATION_STEPS) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(MediQSize.progressBar)
                    .clip(RoundedCornerShape(MediQRadius.pill))
                    .background(if (index < currentStep) MediQGreen else MediQBorder)
            )
        }
    }
}
