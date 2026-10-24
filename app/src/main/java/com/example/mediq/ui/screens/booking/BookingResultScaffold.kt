package com.example.mediq.ui.screens.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mediq.ui.components.MediQStatusMark
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQOnBrand
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing

/*
 * The parts both booking results share: the centred scrolling column, the
 * round mark, and the two button shapes. Keeping them here lets the success
 * and conflict bodies read as one place with two outcomes.
 */

@Composable
internal fun ResultColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = MediQSpacing.gutter,
                vertical = MediQSpacing.lg
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

/** The round mark at the centre of the result. */
@Composable
internal fun ResultMark(
    icon: ImageVector,
    container: Color,
    tint: Color,
    contentDescription: String?
) {
    MediQStatusMark(
        icon = icon,
        container = container,
        tint = tint,
        contentDescription = contentDescription
    )
}

@Composable
internal fun PrimaryResultButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MediQSize.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MediQGreen,
            contentColor = MediQOnBrand
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
internal fun SecondaryResultButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MediQSize.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(MediQSize.hairline, MediQBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MediQGreen
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
