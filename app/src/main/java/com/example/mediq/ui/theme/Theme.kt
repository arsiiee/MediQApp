package com.example.mediq.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MediQGreen,
    secondary = MediQLightGreen,
    tertiary = MediQDarkGreen,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = MediQBackground,
    onSecondary = MediQTextPrimary,
    onTertiary = MediQBackground,
    onBackground = MediQBackground,
    onSurface = MediQBackground,
    error = MediQError
)

private val LightColorScheme = lightColorScheme(
    primary = MediQGreen,
    secondary = MediQLightGreen,
    tertiary = MediQDarkGreen,
    background = MediQBackground,
    surface = MediQSurface,
    onPrimary = MediQBackground,
    onSecondary = MediQGreen,
    onTertiary = MediQBackground,
    onBackground = MediQTextPrimary,
    onSurface = MediQTextPrimary,
    error = MediQError
)

@Composable
fun MediQTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            /*
             * The app chrome behind the system bars is light: Scaffold's container
             * (MediQBackground) sits above the status bar and the bottom bar is
             * white. Light bars therefore need dark icons, or they vanish into the
             * background.
             *
             * If a screen ever draws a dark surface under the status bar, it must
             * flip this for as long as it is visible.
             */
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MediQShapes,
        content = content
    )
}
