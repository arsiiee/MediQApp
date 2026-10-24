package com.example.mediq.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQError
import com.example.mediq.ui.theme.MediQErrorBorder
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/** The green band at the top with the avatar and name. */
@Composable
internal fun ProfileHeader(
    profile: ProfileData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MediQSize.headerBar)
            .background(com.example.mediq.ui.theme.MediQGreen)
    )

    Column(modifier = Modifier.padding(horizontal = MediQSpacing.gutter)) {
        Spacer(Modifier.height(MediQSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(MediQSize.avatar)
                    .clip(CircleShape)
                    .background(MediQLightGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.initials,
                    style = MaterialTheme.typography.titleLarge,
                    color = com.example.mediq.ui.theme.MediQGreen
                )
            }

            Spacer(Modifier.width(MediQSpacing.lg))

            Column {
                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MediQTextPrimary
                )

                Spacer(Modifier.height(MediQSpacing.xxs))

                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MediQTextSecondary
                )
            }
        }
    }
}

/** A label/value row in the personal details card. */
@Composable
internal fun ProfileDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MediQSpacing.md, vertical = MediQSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MediQTextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelMedium, color = MediQTextPrimary)
    }
}

/** The card with the phone number and the edit button. */
@Composable
internal fun ProfileDetailsCard(
    profile: ProfileData,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MediQBackground,
        border = androidx.compose.foundation.BorderStroke(com.example.mediq.ui.theme.MediQSize.hairline, com.example.mediq.ui.theme.MediQBorder)
    ) {
        Column {
            ProfileDetailRow(label = "Mobile number", value = profile.phoneNumber)

            androidx.compose.material3.HorizontalDivider(color = com.example.mediq.ui.theme.MediQBorder, modifier = Modifier.padding(horizontal = MediQSpacing.md))

            androidx.compose.material3.OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MediQSpacing.md)
                    .heightIn(min = MediQSize.minTouchTarget),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(com.example.mediq.ui.theme.MediQSize.hairline, com.example.mediq.ui.theme.MediQBorder),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MediQTextPrimary)
            ) {
                androidx.compose.material3.Icon(imageVector = androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Edit", tint = MediQTextPrimary, modifier = Modifier.size(com.example.mediq.ui.theme.MediQSize.icon))
                Spacer(Modifier.width(MediQSpacing.sm))
                Text(text = "Edit my details", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** The sign-out button. */
@Composable
internal fun SignOutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MediQSize.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(com.example.mediq.ui.theme.MediQSize.hairline, com.example.mediq.ui.theme.MediQErrorBorder),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = com.example.mediq.ui.theme.MediQErrorContainer,
            contentColor = com.example.mediq.ui.theme.MediQError
        )
    ) {
        androidx.compose.material3.Icon(imageVector = androidx.compose.material.icons.Icons.Default.Logout, contentDescription = "Sign out", tint = com.example.mediq.ui.theme.MediQError, modifier = Modifier.size(com.example.mediq.ui.theme.MediQSize.icon))
        Spacer(Modifier.width(MediQSpacing.sm))
        Text(text = "Sign out", style = MaterialTheme.typography.labelLarge, color = com.example.mediq.ui.theme.MediQError)
    }
}