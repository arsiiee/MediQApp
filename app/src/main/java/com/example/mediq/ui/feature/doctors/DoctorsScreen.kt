package com.example.mediq.ui.feature.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Money
import com.example.mediq.domain.model.Specialty
import com.example.mediq.ui.navigation.Screen

@Composable
fun DoctorsScreen(navController: NavController) {
    val viewModel: DoctorsViewModel = viewModel(factory = DoctorsViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMediQColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Search doctor name or specialty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = state.searchText,
            onValueChange = viewModel::onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.outline
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.selectedSpecialty != null,
                onClick = {
                    // Tapping an active chip clears it, so there's a way back to
                    // the unfiltered list.
                    val next = if (state.selectedSpecialty != null) null else Specialty.PEDIATRICS
                    viewModel.onSpecialtySelected(next)
                },
                label = { Text("Filter by specialty") }
            )
            FilterChip(
                selected = false,
                onClick = {},
                enabled = false,
                label = { Text("Filter by clinic location") }
            )
        }

        when (val doctors = state.doctors) {
            is LoadState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.accent)
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
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

@Composable
private fun DoctorListItem(doctor: Doctor, onClick: () -> Unit) {
    val colors = LocalMediQColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MediQLightGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = doctor.initials, color = MediQGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(text = doctor.displayName, fontWeight = FontWeight.Bold)
                Text(text = doctor.specialty.displayName, color = colors.secondaryText, fontSize = 14.sp)
                Text(
                    text = "${doctor.location.building} — ${doctor.location.floor} — ${doctor.location.room}",
                    color = colors.secondaryText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Experience", color = colors.secondaryText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "${doctor.yearsOfExperience} years",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(text = "Consultation", color = colors.secondaryText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = doctor.consultationFee.format(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Formats centavos as pesos. Lives here because display is the UI's job. */
private fun Money.format(): String = "₱$pesos"