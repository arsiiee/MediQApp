package com.example.mediq.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Every ratio in the comments below is measured with the WCAG 2.1 relative
// luminance formula, not eyeballed. If you change a value, re-measure it: a
// token that used to pass can stop passing without anything looking wrong.

// ── Brand surfaces ──────────────────────────────────────────────────────────
// These are *container* colours. They are always paired with [MediQOnBrand].
// Do not use them for text or icons on a themed background - that needs
// [MediQAccentLight] or [MediQAccentDark], which exist because a single green
// cannot clear 4.5:1 on both #FFFFFF and #121212.

/** Filled brand surface. [MediQOnBrand] on it is 6.63:1. */
val MediQGreen = Color(0xFF006B3C)

/** Tinted brand surface. [MediQGreen] on it is 5.89:1. */
val MediQLightGreen = Color(0xFFE8F5E9)

/** Deep brand surface. [MediQOnBrand] on it is 10.01:1. */
val MediQDarkGreen = Color(0xFF004D2C)

/** Content colour placed on [MediQGreen], [MediQDarkGreen] or white. */
val MediQOnBrand = Color(0xFFFFFFFF)

// ── Accent ──────────────────────────────────────────────────────────────────
// For text, icons, progress indicators and tab content that sits directly on
// the themed background. Needs to change with the mode; see [MediQAccentLight].

/** Accent on a light background: 6.63:1 on #FFFFFF. */
val MediQAccentLight = Color(0xFF006B3C)

/** Accent on a dark background: 10.47:1 on #121212, 9.32:1 on #1E1E1E. */
val MediQAccentDark = Color(0xFF6FD696)

// ── Text ────────────────────────────────────────────────────────────────────

val MediQTextPrimary = Color(0xFF212121)

/** Secondary text, light background: 6.39:1 on #FFFFFF, 5.86:1 on #F5F5F5. */
val MediQTextSecondary = Color(0xFF5F5F5F)

/** Secondary text, dark background: 6.99:1 on #121212, 6.22:1 on #1E1E1E. */
val MediQTextSecondaryDark = Color(0xFF9E9E9E)

/** Page background, light mode only. Use [MediQOnBrand] to draw on it. */
val MediQBackground = Color(0xFFFFFFFF)

/** Raised surface, light mode only. [MediQTextSecondary] on it is 5.86:1. */
val MediQSurface = Color(0xFFF5F5F5)

/** Raised surface in dark mode. [MediQTextSecondaryDark] on it is 5.36:1. */
val MediQSurfaceVariantDark = Color(0xFF2A2A2A)

/** Tinted container in dark mode. [MediQOnBrand] on it is 10.01:1. */
val MediQContainerDark = Color(0xFF004D2C)

// ── Lines ───────────────────────────────────────────────────────────────────

/** 1dp outline, light: 3.45:1 on #FFFFFF, the WCAG 1.4.11 floor for non-text. */
val MediQOutline = Color(0xFF8A8A8A)

/** 1dp outline, dark: 6.99:1 on #121212. */
val MediQOutlineDark = Color(0xFF9E9E9E)

// ── Status ──────────────────────────────────────────────────────────────────

/** 7.33:1 on white, 6.72:1 on #F5F5F5. */
val MediQError = Color(0xFFB00020)

/**
 * Error in dark mode. The light-mode red measures 1.96:1 on the dark raised
 * surface, so it has to lighten to stay readable: 5.17:1 on #2A2A2A.
 */
val MediQErrorDark = Color(0xFFFF6B6B)

/** 5.13:1 on white. Was #388E3C, which measured 4.12:1 and failed AA. */
val MediQSuccess = Color(0xFF2E7D32)

/** 4.67:1 on white. Was #FBC02D, which measured 1.66:1 and was unreadable. */
val MediQWarning = Color(0xFFB25E00)