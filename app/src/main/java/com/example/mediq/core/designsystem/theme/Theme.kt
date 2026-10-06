package com.example.mediq.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Colour roles that Material's [androidx.compose.material3.ColorScheme] has no
 * slot for: the accent drawn on the themed background, secondary text, and
 * outlines.
 *
 * These live outside [ColorScheme] on purpose. `primary` has to stay the dark
 * brand green in both schemes, because filled buttons hardcode
 * `containerColor = MediQGreen` and take their label colour from `onPrimary` -
 * making `primary` light in dark mode would put white-on-pale-green labels on
 * every button (1.79:1). The accent therefore needs a home of its own that is
 * allowed to change with the mode.
 */
@Immutable
data class MediQExtendedColors(
    /** Text, icons, progress, tab content drawn on the themed background. */
    val accent: Color,
    /** Supporting text: metadata, timestamps, helper lines. */
    val secondaryText: Color,
    /** 1dp borders and dividers. */
    val outline: Color,
)

val LightExtendedColors = MediQExtendedColors(
    accent = MediQAccentLight,
    secondaryText = MediQTextSecondary,
    outline = MediQOutline,
)

val DarkExtendedColors = MediQExtendedColors(
    accent = MediQAccentDark,
    secondaryText = MediQTextSecondaryDark,
    outline = MediQOutlineDark,
)

val LocalMediQColors = staticCompositionLocalOf { LightExtendedColors }

// Every container and neutral slot the app actually reads is set explicitly.
// Anything left to the Material baseline arrives as baseline *purple*
// (#EADDFF, #E7E0EC, #FFD8E4), which is both off-brand in a green clinical app
// and, for `primary` on `surfaceVariant`, unreadable in dark mode at 1.41:1.
private val DarkColorScheme = darkColorScheme(
    primary = MediQGreen,
    secondary = MediQLightGreen,
    tertiary = MediQDarkGreen,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = MediQOnBrand,
    onSecondary = MediQTextPrimary,
    onTertiary = MediQOnBrand,
    onBackground = MediQOnBrand,
    onSurface = MediQOnBrand,
    primaryContainer = MediQContainerDark,
    onPrimaryContainer = MediQOnBrand,
    secondaryContainer = MediQLightGreen,
    onSecondaryContainer = MediQTextPrimary,
    tertiaryContainer = MediQContainerDark,
    onTertiaryContainer = MediQOnBrand,
    surfaceVariant = MediQSurfaceVariantDark,
    onSurfaceVariant = MediQTextSecondaryDark,
    outline = MediQOutlineDark,
    outlineVariant = Color(0xFF49454F),
    error = MediQErrorDark,
    onError = Color(0xFF3A0708)
)

private val LightColorScheme = lightColorScheme(
    primary = MediQGreen,
    secondary = MediQLightGreen,
    tertiary = MediQDarkGreen,
    background = MediQBackground,
    surface = MediQSurface,
    onPrimary = MediQOnBrand,
    onSecondary = MediQAccentLight,
    onTertiary = MediQOnBrand,
    onBackground = MediQTextPrimary,
    onSurface = MediQTextPrimary,
    primaryContainer = MediQLightGreen,
    onPrimaryContainer = MediQAccentLight,
    secondaryContainer = MediQLightGreen,
    onSecondaryContainer = MediQTextPrimary,
    tertiaryContainer = MediQLightGreen,
    onTertiaryContainer = MediQAccentLight,
    surfaceVariant = MediQSurface,
    onSurfaceVariant = MediQTextSecondary,
    outline = MediQOutline,
    outlineVariant = Color(0xFFD9D9D9),
    error = MediQError,
    onError = MediQOnBrand
)

@Composable
fun MediQTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // `primary` is the same dark brand green in both schemes, so the bar
            // is always dark and its icons are always light. Keying this off
            // `darkTheme` (as it used to) drew dark icons on the dark green bar
            // in dark mode, which measured 3.17:1.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalMediQColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}