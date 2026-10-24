package com.example.mediq.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQAvatarBrandTile
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import com.example.mediq.ui.theme.MediQTextPlaceholder

/** One row in the list. */
@Composable
internal fun ConversationCard(
    conversation: SecretaryConversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MediQBackground
    ) {
        Row(
            modifier = Modifier.padding(MediQSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(MediQSize.avatar)
                    .clip(RoundedCornerShape(com.example.mediq.ui.theme.MediQRadius.pill))
                    .background(MediQAvatarBrandTile),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.initials,
                    style = MaterialTheme.typography.titleSmall,
                    color = MediQGreen
                )
            }

            Spacer(Modifier.width(MediQSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.secretaryName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MediQTextPrimary
                )

                Spacer(Modifier.height(MediQSpacing.xxs))

                Text(
                    text = "Secretary · ${conversation.doctorName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MediQGreen
                )

                Spacer(Modifier.height(MediQSpacing.xxs))

                Text(
                    text = conversation.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MediQTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(MediQSpacing.xxs))

                Text(
                    text = conversation.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MediQTextSecondary
                )
            }

            if (conversation.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(MediQSize.conversationIcon)
                        .clip(RoundedCornerShape(com.example.mediq.ui.theme.MediQRadius.pill))
                        .background(MediQGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MediQOnBrand
                    )
                }
            }
        }
    }
}