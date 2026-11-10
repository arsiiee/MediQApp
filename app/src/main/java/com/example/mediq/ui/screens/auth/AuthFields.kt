package com.example.mediq.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPlaceholder
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * A labelled text field with somewhere to put the error.
 *
 * The error goes in the field's own supporting slot and flips
 * [OutlinedTextField]'s error flag, so it is announced with the field rather
 * than only drawn under it. When there is no error the same slot carries the
 * [hint], which keeps the label from having to repeat itself.
 */
@Composable
internal fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MediQTextPrimary
        )

        Spacer(Modifier.height(MediQSpacing.sm))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error != null,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediQTextPlaceholder
                )
            },
            supportingText = supportText(error = error, hint = hint),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = trailingIcon,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MediQTextPrimary),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MediQBackground,
                unfocusedContainerColor = MediQBackground,
                focusedBorderColor = if (error != null) MediQError else MediQGreen,
                unfocusedBorderColor = if (error != null) MediQError else MediQBorder,
                cursorColor = MediQGreen
            )
        )
    }
}

/** A password field: [AuthField] plus the show/hide control. */
@Composable
internal fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    AuthField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        error = error,
        modifier = modifier,
        hint = hint,
        enabled = enabled,
        keyboardType = KeyboardType.Password,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = MediQTextSecondary
                )
            }
        }
    )
}

/** The error when there is one, otherwise the hint, otherwise nothing. */
@Composable
private fun supportText(error: String?, hint: String?): (@Composable () -> Unit)? {
    val text = error ?: hint ?: return null
    return {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (error != null) MediQError else MediQTextSecondary
        )
    }
}
