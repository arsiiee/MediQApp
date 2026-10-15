package com.example.mediq.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mediq.core.designsystem.theme.LocalMediQColors

/**
 * A label on the left, its value on the right, one row per pair, a hairline
 * between rows. The shape the appointment-details facts are drawn in.
 *
 * Pairs rather than a fixed record so a screen adds a row by adding a pair: this
 * component must not know which facts a particular appointment has, because then
 * it would have to change every time a fact is added.
 */
@Composable
fun MediQKeyValueGrid(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMediQColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) MediQDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryText,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text      = value,
                    modifier  = Modifier.weight(1f),
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
