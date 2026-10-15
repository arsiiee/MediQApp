package com.example.mediq.ui.feature.doctors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.component.MediQAvatar
import com.example.mediq.core.designsystem.component.MediQBrandAppBar
import com.example.mediq.core.designsystem.component.MediQDivider
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQOnBrand
import com.example.mediq.core.designsystem.theme.MediQOnBrandMuted
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.model.nextClinicSession
import com.example.mediq.ui.format.format
import com.example.mediq.ui.navigation.Screen
import java.time.DayOfWeek
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun DoctorsScreen(navController: NavController) {
    val viewModel: DoctorsViewModel = viewModel(factory = DoctorsViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MediQBrandAppBar(
                title = "Doctors",
                actions = {
                    // The figure draws a count and the list already holds it, so no
                    // server total is needed while pagination is unused.
                    val count = (state.doctors as? LoadState.Success)?.data?.size
                    if (count != null) {
                        Text(text = "$count results", color = MediQOnBrandMuted)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SearchField(
                value = state.searchText,
                onValueChange = viewModel::onSearchTextChange,
                onClear = { viewModel.onSearchTextChange("") },
            )
            Spacer(modifier = Modifier.height(16.dp))
            SpecialtyFilterRow(
                selected = state.selectedSpecialty,
                onSelect = viewModel::onSpecialtySelected,
            )
            Spacer(modifier = Modifier.height(4.dp))

            when (val doctors = state.doctors) {
                is LoadState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // `accent`, not `MediQGreen`: the brand green on a dark
                    // background is ~1.9:1, so the spinner would vanish.
                    CircularProgressIndicator(color = LocalMediQColors.current.accent)
                }

                is LoadState.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = doctors.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                is LoadState.Success -> {
                    if (doctors.data.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.PersonSearch,
                            title = if (state.searchText.isBlank()) "No doctors yet" else "No matches",
                            description = if (state.searchText.isBlank()) {
                                "Doctors will appear here once the directory is available."
                            } else {
                                "Nothing matched \"${state.searchText}\". Try a different name or specialty."
                            },
                            actionLabel = if (state.searchText.isBlank()) "Check again" else null,
                            onAction = if (state.searchText.isBlank()) { { viewModel.refresh() } } else null,
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(doctors.data) { doctor ->
                                DoctorListItem(
                                    doctor = doctor,
                                    onClick = {
                                        navController.navigate(Screen.DoctorDetails.createRoute(doctor.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The pill search box: a rounded field with a leading magnifier and a clear
 * button.
 *
 * Deliberately not the shared `MediQBrandTextField`, which draws a label *above*
 * its box because it is a form field. The figure's search box has no label and is
 * a pill (`rx=21`), and its clear button is only wanted when there is something
 * to clear - so this is its own composable rather than a mode bolted onto the
 * shared field.
 */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMediQColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search doctor or specialty", color = colors.secondaryText) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = colors.secondaryText)
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = colors.secondaryText,
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(21.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = colors.outline,
            unfocusedBorderColor = colors.outline,
        ),
    )
}

/**
 * One chip per [Specialty], plus an "All" chip that clears the filter.
 *
 * The chip list is the enum rather than a server read, and that is deliberate.
 * `ReferenceData` seeds the server's `specialties` table from the same enum at
 * every boot, so the two cannot disagree - a `GET /specialties` route could only
 * ever return what the app already holds, at the cost of an endpoint, a DTO, a
 * repository method, and a loading state for a list that cannot differ. The
 * labels come from `displayName`, so a chip reads "Dermatology" and the wire
 * value never reaches the UI.
 *
 * "All" is not decoration: the row is seven chips wide, so without it the only
 * route back to the unfiltered list is to remember which chip is lit. Tapping the
 * lit chip clears it as well.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecialtyFilterRow(
    selected: Specialty?,
    onSelect: (Specialty?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MediQGreen,
                selectedLabelColor = MediQOnBrand,
            ),
        )
        Specialty.entries.forEach { specialty ->
            FilterChip(
                selected = selected == specialty,
                // Tapping the lit chip clears it, so the filter toggles off
                // without the "All" chip being the only way back.
                onClick = { onSelect(if (selected == specialty) null else specialty) },
                label = { Text(specialty.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MediQGreen,
                    selectedLabelColor = MediQOnBrand,
                ),
            )
        }
    }
}

/**
 * One row of the list: who the doctor is, what they cost, and when they are next
 * open.
 *
 * `Next open` comes from the doctor's own weekly `clinicHours` via
 * [nextClinicSession] rather than from an availability request, because a
 * per-row request is N+1 reads on a list. The `4 slots` the figure draws beside
 * it is deliberately absent: only `getAvailability` knows that count, and the
 * list does not need it.
 */
@Composable
private fun DoctorListItem(doctor: Doctor, onClick: () -> Unit) {
    val colors = LocalMediQColors.current
    // Read once per composition and rounded to the minute, so rows cannot
    // disagree about "now" by a few milliseconds.
    val now = remember { Instant.now().truncatedTo(ChronoUnit.MINUTES) }
    val nextOpen = remember(doctor.clinicHours, now) {
        nextClinicSession(doctor.clinicHours, now)
    }

    Surface(
        // `background` rather than `surface`: in light mode `surface` is
        // #F5F5F5, which is the fill the PRC pill uses, so a `surface` card
        // would swallow its own badge. The figure draws a white card on a white
        // page and separates them with the border.
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row {
                MediQAvatar(initials = doctor.initials, size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doctor.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "${doctor.specialty.displayName} \u00b7 ${doctor.yearsOfExperience} yrs",
                        color = colors.secondaryText,
                        fontSize = 11.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PrcBadge(doctor.licenseNumber)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        // `accent`, not the brand green: the fee must stay legible
                        // in dark mode, and the brand green on #121212 does not.
                        text = doctor.consultationFee.format(),
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "consultation",
                        color = colors.secondaryText,
                        fontSize = 10.sp,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.outline,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            MediQDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = listOf(doctor.location.building, doctor.location.floor, doctor.location.room)
                    .joinToString(" \u00b7 "),
                color = colors.secondaryText,
                fontSize = 11.sp,
            )
            if (nextOpen != null) {
                Text(
                    text = "Next open: ${nextOpen.dayOfWeek.shortLabel()} " +
                        nextOpen.opensAt.format(TIME_OF_DAY),
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/** `PRC . DEMO-PRC-0001` in a tinted pill, from the doctor's real licence number. */
@Composable
private fun PrcBadge(licenseNumber: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(9.dp),
    ) {
        Text(
            text = "PRC \u00b7 $licenseNumber",
            color = LocalMediQColors.current.secondaryText,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

private val TIME_OF_DAY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** "Tue" - the figure's day label, fixed to English rather than the phone's locale. */
private fun DayOfWeek.shortLabel(): String = getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
