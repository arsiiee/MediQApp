package com.example.mediq.ui.screens.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQRadius
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/** Section heading inside the home scroll. */
@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MediQTextPrimary
    )
}

/** Icon plus supporting copy on one line, e.g. the clinic location or arrival note. */
@Composable
internal fun MetaRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MediQTextSecondary,
            modifier = Modifier.size(MediQSize.icon)
        )
        Spacer(Modifier.width(MediQSpacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MediQTextSecondary
        )
    }
}

/**
 * The home screen's single primary action.
 *
 * Carries text as well as an icon, and is sized at the 48dp minimum touch target.
 */
@Composable
internal fun BookConsultationButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(MediQSize.minTouchTarget),
        shape = RoundedCornerShape(MediQRadius.md)
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = MediQOnBrand,
            modifier = Modifier.size(MediQSize.iconLarge)
        )
        Spacer(Modifier.width(MediQSpacing.sm))
        Text(
            text = "Book a consultation",
            style = MaterialTheme.typography.labelLarge
        )
    }
}
