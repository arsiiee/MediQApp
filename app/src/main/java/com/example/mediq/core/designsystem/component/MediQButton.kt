package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
 */
@Composable
fun MediQButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MediQButtonVariant = MediQButtonVariant.Primary,
    enabled: Boolean = true,
) {
    when (variant) {
        MediQButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MediQGreen,
                contentColor = MediQOnBrand,
            ),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        MediQButtonVariant.Outline -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LocalMediQColors.current.accent,
            ),
            border = BorderStroke(1.dp, LocalMediQColors.current.accent),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
