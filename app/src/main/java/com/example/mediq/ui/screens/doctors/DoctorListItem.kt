package com.example.mediq.ui.screens.doctors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.mediq.ui.theme.MediQAvatarBrandTile
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQElevation
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * One doctor in the result list.
 *
 * The whole row is the target, so it carries an `onClickLabel` naming the
 * doctor; without it a screen reader would read only the row's text.
 */
@Composable
fun DoctorListItem(
    doctor: DoctorInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "View ${doctor.name}",
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MediQBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = MediQElevation.card),
        border = BorderStroke(MediQSize.hairline, MediQBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MediQSize.minTouchTarget)
                .padding(MediQSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {

            InitialsAvatar(initials = doctor.initials)

            Spacer(Modifier.width(MediQSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = doctor.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MediQTextPrimary
                )

                Spacer(Modifier.height(MediQSpacing.xxs))

                Text(
                    text = doctor.specialty,
                    style = MaterialTheme.typography.labelMedium,
                    color = MediQGreen
                )

                Spacer(Modifier.height(MediQSpacing.xs))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MediQTextSecondary,
                        modifier = Modifier.size(MediQSize.icon)
                    )
                    Spacer(Modifier.width(MediQSpacing.xs))
                    Text(
                        text = doctor.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MediQTextSecondary
                    )
                }

                Spacer(Modifier.height(MediQSpacing.sm))

                Text(
                    text = "${doctor.openSlots} open slots",
                    style = MaterialTheme.typography.labelMedium,
                    color = MediQGreen
                )
            }
        }
    }
}

@Composable
private fun InitialsAvatar(initials: String) {
    Box(
        modifier = Modifier
            .size(MediQSize.avatar)
            .clip(CircleShape)
            .background(MediQAvatarBrandTile),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MediQTextPrimary
        )
    }
}
