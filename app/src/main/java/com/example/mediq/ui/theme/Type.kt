package com.example.mediq.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.mediq.R

val Inter = FontFamily(
    Font(R.font.inter_extralight, FontWeight.W100, FontStyle.Normal),
    Font(R.font.inter_regular, FontWeight.W400, FontStyle.Normal),
    Font(R.font.inter_medium, FontWeight.W500, FontStyle.Normal),
    Font(R.font.inter_semibold, FontWeight.W600, FontStyle.Normal),
    Font(R.font.inter_bold, FontWeight.W700, FontStyle.Normal),
    Font(R.font.inter_extrabold, FontWeight.W800, FontStyle.Normal)
)

val Judson = FontFamily(
    Font(R.font.judson_regular, FontWeight.W400, FontStyle.Normal)
)

/*
 * MediQ type scale.
 *
 * Rules:
 *  - Screens read `MaterialTheme.typography.*`. No inline `fontSize` or `fontWeight`.
 *  - Heading levels are not skipped: a screen has one titleLarge, then titleSmall
 *    for its sections, then body styles for content.
 *
 * Role map:
 *  - headlineSmall  appointment time, the one number that matters.
 *  - titleLarge     the single page title.
 *  - titleMedium    the greeting, prominent person names.
 *  - titleSmall     section headings inside a screen.
 *  - labelLarge     card titles, list primary text, button labels.
 *  - bodyMedium     paragraph copy.
 *  - bodySmall      metadata: dates, specialties, supporting lines.
 *  - labelMedium    status pills, chips.
 *  - labelSmall     the tightest metadata: location lines.
 */
val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/** Judson wordmark style for the "MediQ" logo. */
val Wordmark = TextStyle(
    fontFamily = Judson,
    fontWeight = FontWeight.Normal,
    fontSize = 40.sp,
    lineHeight = 48.sp,
    letterSpacing = 0.sp
)
