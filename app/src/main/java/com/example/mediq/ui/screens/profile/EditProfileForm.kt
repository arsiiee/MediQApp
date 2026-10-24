package com.example.mediq.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPlaceholder
import com.example.mediq.ui.theme.MediQTextPrimary

/** The edit-profile form fields. */
@Composable
internal fun EditProfileForm(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "Full name", style = MaterialTheme.typography.labelLarge, color = MediQTextPrimary)
        Spacer(Modifier.height(MediQSpacing.sm))
        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MediQBackground,
                unfocusedContainerColor = MediQBackground,
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = MediQBorder,
                cursorColor = MediQGreen
            )
        )

        Spacer(Modifier.height(MediQSpacing.lg))

        Text(text = "Phone number", style = MaterialTheme.typography.labelLarge, color = MediQTextPrimary)
        Spacer(Modifier.height(MediQSpacing.sm))
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MediQBackground,
                unfocusedContainerColor = MediQBackground,
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = MediQBorder,
                cursorColor = MediQGreen
            )
        )
    }
}

/** The edit-profile save button. */
@Composable
internal fun EditProfileSaveButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MediQSize.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = com.example.mediq.ui.theme.MediQGreen,
            contentColor = com.example.mediq.ui.theme.MediQOnBrand,
            disabledContainerColor = com.example.mediq.ui.theme.MediQDisabledContainer,
            disabledContentColor = com.example.mediq.ui.theme.MediQOnBrand
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}