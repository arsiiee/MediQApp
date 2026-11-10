package com.example.mediq.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Material 3 shape scale, mapped onto [MediQRadius].
 *
 * Screens should read `MaterialTheme.shapes.large` for a card rather than
 * constructing a `RoundedCornerShape` by hand.
 */
val MediQShapes = Shapes(
    extraSmall = RoundedCornerShape(MediQRadius.sm),
    small = RoundedCornerShape(MediQRadius.sm),
    medium = RoundedCornerShape(MediQRadius.md),
    large = RoundedCornerShape(MediQRadius.lg),
    extraLarge = RoundedCornerShape(MediQRadius.lg)
)
