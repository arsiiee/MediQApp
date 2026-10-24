package com.example.mediq.ui.screens.doctors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQFieldContainer
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQIconMuted
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQSpacing
import com.example.mediq.ui.theme.MediQTextPlaceholder
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

/**
 * Search field plus the two filter menus.
 *
 * The result count is announced here rather than only shown in the list, so a
 * screen reader user learns the effect of a filter without scrolling.
 */
@Composable
fun DoctorsSearchControls(
    filters: DoctorFilters,
    onFiltersChange: (DoctorFilters) -> Unit,
    specialtyMenuExpanded: Boolean,
    onSpecialtyMenuExpandedChange: (Boolean) -> Unit,
    locationMenuExpanded: Boolean,
    onLocationMenuExpandedChange: (Boolean) -> Unit,
    resultCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

        OutlinedTextField(
            value = filters.query,
            onValueChange = { onFiltersChange(filters.copy(query = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    // The placeholder is not a label: name the field explicitly.
                    contentDescription = "Search doctors by name or specialty"
                },
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search doctor name or specialty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediQTextPlaceholder
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MediQIconMuted
                )
            },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MediQFieldContainer,
                unfocusedContainerColor = MediQFieldContainer,
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = MediQBorder,
                cursorColor = MediQGreen
            )
        )

        Spacer(Modifier.height(MediQSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MediQSpacing.sm)
        ) {
            FilterDropdown(
                label = "Specialty",
                text = filters.specialty,
                options = specialtyOptions,
                expanded = specialtyMenuExpanded,
                onExpandedChange = onSpecialtyMenuExpandedChange,
                onOptionSelected = { onFiltersChange(filters.copy(specialty = it)) },
                modifier = Modifier.weight(1f)
            )

            FilterDropdown(
                label = "Location",
                text = filters.location,
                options = locationOptions,
                expanded = locationMenuExpanded,
                onExpandedChange = onLocationMenuExpandedChange,
                onOptionSelected = { onFiltersChange(filters.copy(location = it)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(MediQSpacing.md))

        Text(
            text = resultCountLabel(resultCount),
            style = MaterialTheme.typography.labelMedium,
            color = MediQTextSecondary
        )
    }
}

private fun resultCountLabel(resultCount: Int): String = when (resultCount) {
    0 -> "No doctors match"
    1 -> "1 doctor"
    else -> "$resultCount doctors"
}

/**
 * A labelled filter menu.
 *
 * [label] is a visual prefix so the value alone is not mistaken for the
 * control's name; the accessible name comes from the button's own text.
 */
@Composable
private fun FilterDropdown(
    label: String,
    text: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {

        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MediQSize.minTouchTarget),
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = MediQSpacing.md),
            border = BorderStroke(
                width = MediQSize.hairline,
                color = if (expanded) MediQGreen else MediQBorder
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MediQBackground,
                contentColor = MediQTextPrimary
            )
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MediQTextSecondary
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MediQTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(MediQSpacing.xs))

            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MediQTextSecondary,
                modifier = Modifier.size(MediQSize.icon)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}
