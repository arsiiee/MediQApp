package com.example.mediq.ui.feature.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentStatus
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.toClinicDate
import com.example.mediq.domain.model.toClinicTime
import com.example.mediq.ui.navigation.Screen
import java.time.format.DateTimeFormatter

@Composable
fun AppointmentsScreen(navController: NavController) {
    val viewModel: AppointmentsViewModel = viewModel(factory = AppointmentsViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val tabs = listOf("Upcoming", "History")
    val colors = LocalMediQColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "My appointments",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        PrimaryTabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = Color.Transparent,
            contentColor = colors.accent,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick  = { viewModel.onTabSelected(index) },
                    text = { Text(text = title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val current = uiState.current) {
            is LoadState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.CenterHorizontally),
                    color = colors.accent,
                )
            }

            is LoadState.Error -> {
                EmptyState(
                    icon = if (uiState.selectedTab == 0)
                        Icons.Outlined.EventAvailable else Icons.Outlined.History,
                    title = "Couldn't load appointments",
                    description = current.message,
                )
            }

            is LoadState.Success -> {
                if (current.data.isEmpty()) {
                    if (uiState.selectedTab == 0) {
                        EmptyState(
                            icon = Icons.Outlined.EventAvailable,
                            title = "No upcoming appointments",
                            description = "Book a consultation and it will show up here.",
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.History,
                            title = "No past appointments",
                            description = "Completed and cancelled consultations will be listed here.",
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                        items(current.data, key = { it.id }) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                onClick = {
                                    navController.navigate(
                                        Screen.AppointmentDetails.createRoute(appointment.id)
                                    )
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
private fun AppointmentCard(appointment: Appointment, onClick: () -> Unit) {
    val colors = LocalMediQColors.current
    val date = appointment.startsAt.toClinicDate()
    val time = appointment.startsAt.toClinicTime()

    Surface(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        color     = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Date badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MediQLightGreen,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text  = date.format(DateTimeFormatter.ofPattern("MMM")).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MediQGreen,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text  = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MediQGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = appointment.doctor.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text  = appointment.doctor.specialty.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
                Text(
                    text  = time.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
            }

            // Status badge
            StatusChip(appointment.status)
        }
    }
}

@Composable
private fun StatusChip(status: AppointmentStatus) {
    // Both halves of every pair are hardcoded, so these chips keep the same
    // measured contrast in either mode. The amber was #F9A825 on #FFF8E1,
    // which is 1.85:1 - unreadable at 10sp.
    val (bg, fg) = when (status) {
        AppointmentStatus.CONFIRMED           -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        AppointmentStatus.PENDING_CONFIRMATION -> Color(0xFFFFF8E1) to Color(0xFF8F5000)
        AppointmentStatus.COMPLETED           -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        AppointmentStatus.CANCELLED           -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        AppointmentStatus.DECLINED            -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        // Grey, because the meaning is not known. Reusing a status colour here
        // would imply a conclusion the app has not reached.
        AppointmentStatus.UNKNOWN             -> Color(0xFFEEEEEE) to Color(0xFF616161)
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(
            text     = status.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = fg,
            fontSize = 10.sp,
        )
    }
}