package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand

/** The two button shapes the figures use. */
enum class MediQButtonVariant { Primary, Outline }

/**
 * A primary (filled) or secondary (outlined) action.
 *
 * The filled variant fixes [MediQGreen] as its container and [MediQOnBrand] as
 * its label in both modes, matching the buttons already in the app. That is why
 * the theme's `primary` cannot lighten in dark mode - see `MediQExtendedColors`.
 * The outlined variant draws its label and border in the mode-dependent accent so
 * it stays legible on either page background.
 *
 * [loading] swaps the label for a spinner and disables the button. It lives here
 * because it is part of the app's button vocabulary: a screen that renders
 * `isLoading` must keep doing so, and a component that offered only a `String`
 * would silently drop that state.
 */
@Composable
fun MediQButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MediQButtonVariant = MediQButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interactive = enabled && !loading
    when (variant) {
        MediQButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = interactive,
            colors = ButtonDefaults.buttonColors(
                containerColor = MediQGreen,
                contentColor = MediQOnBrand,
            ),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MediQOnBrand,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        MediQButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = interactive,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LocalMediQColors.current.accent,
            ),
            border = BorderStroke(1.dp, LocalMediQColors.current.accent),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = LocalMediQColors.current.accent,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
