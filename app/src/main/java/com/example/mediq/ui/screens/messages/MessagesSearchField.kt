package com.example.mediq.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPlaceholder
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/** The search field pinned to the top of the list. */
@Composable
internal fun MessagesSearchField(
    query: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MediQSize.headerBar)
            .background(MediQGreen)
            .padding(
                horizontal = MediQSpacing.gutter,
                vertical = MediQSpacing.md
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = MediQSpacing.md),
            singleLine = true,
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = MediQTextPrimary),
            placeholder = {
                Text(
                    text = "Search clinic chats...",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = MediQTextPlaceholder
                )
            },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Search,
                    contentDescription = null,
                    tint = com.example.mediq.ui.theme.MediQTextSecondary,
                    modifier = Modifier.size(MediQSize.icon)
                )
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(MediQRadius.sm),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MediQBackground,
                unfocusedContainerColor = MediQBackground,
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = MediQBackground,
                cursorColor = MediQGreen
            )
        )
    }
}