package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQDisabledContainer
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize

/*
 * The two button shapes every auth step uses, so the primary action of five
 * screens is the same control in the same place.
 */

/** The step's primary action. Its label says what pressing it will do. */
@Composable
internal fun AuthPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MediQSize.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MediQGreen,
            contentColor = MediQOnBrand,
            disabledContainerColor = MediQDisabledContainer,
            disabledContentColor = MediQOnBrand
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/** The way out of a step: a route that is not this screen's job. */
@Composable
internal fun AuthSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MediQSize.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(MediQSize.hairline, MediQBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MediQGreen)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}
