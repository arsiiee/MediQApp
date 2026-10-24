package com.example.mediq.ui.screens.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.example.mediq.ui.theme.MediQDisabledContainer
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextSecondary

/*
 * The two controls of the confirm-a-slot form, kept beside it so the form
 * itself stays readable.
 */

/**
 * The tick and its sentence are one control: tapping the words ticks the box,
 * so the patient does not have to aim at a 18dp square.
 */
@Composable
internal fun ConfirmationRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MediQSize.minTouchTarget)
            .clickable(
                enabled = enabled,
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MediQGreen,
                uncheckedColor = MediQTextSecondary
            )
        )

        Text(
            text = "I confirm this booking and will arrive 15 minutes early.",
            style = MaterialTheme.typography.bodySmall,
            color = MediQTextSecondary,
            modifier = Modifier.padding(start = MediQSpacing.xs)
        )
    }
}

/** Primary action. Its label says what pressing it will do. */
@Composable
internal fun ConfirmButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
