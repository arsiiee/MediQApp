package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mediq.core.designsystem.theme.LocalMediQColors

/**
 * The one text field: a label above the box, an optional placeholder and icons.
 *
 * It takes [keyboardOptions] and [keyboardActions] as parameters rather than
 * hiding them, which keeps the secret path explicit - a caller that passes
 * `PasswordVisualTransformation` must also pass `KeyboardType.Password` and
 * `autoCorrectEnabled = false`, and `check-boundaries.ps1` Rule 4 fails the build
 * when it does not. That rule exists because a masked field built on
 * `KeyboardOptions.Default` let Android rewrite a correct password as it was
 * typed. [keyboardActions] is here for the same reason in reverse: a caller that
 * submits a form from the keyboard's `Done` key must be able to say so, or
 * adopting this field would quietly drop that behaviour.
 */
@Composable
fun MediQBrandTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            color = LocalMediQColors.current.secondaryText,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            placeholder = if (placeholder != null) {
                { Text(placeholder) }
            } else {
                null
            },
            leadingIcon = if (leadingIcon != null) {
                { Icon(imageVector = leadingIcon, contentDescription = null) }
            } else {
                null
            },
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            enabled = enabled,
        )
    }
}
