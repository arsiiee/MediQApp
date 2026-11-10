package com.example.mediq.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPlaceholder
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * Search field rendered on the scaffold surface, outside the brand-green
 * header bar. Token-driven: no colours or sizes inline.
 */
@Composable
fun MediQSearchField(
    query: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search..."
) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MediQSpacing.gutter),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MediQTextPrimary),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextPlaceholder
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MediQTextSecondary,
                modifier = Modifier.size(MediQSize.icon)
            )
        },
        shape = RoundedCornerShape(MediQRadius.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MediQBackground,
            unfocusedContainerColor = MediQBackground,
            focusedBorderColor = MediQGreen,
            unfocusedBorderColor = MediQBorder,
            cursorColor = MediQGreen
        )
    )
}
