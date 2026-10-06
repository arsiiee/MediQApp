package com.example.mediq.ui.feature.home

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.component.EmptyState
import com.example.mediq.core.designsystem.component.EmptyStateInline
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.core.designsystem.theme.MediQSurface
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.toClinicDate
import com.example.mediq.domain.model.toClinicTime
import com.example.mediq.ui.navigation.Screen
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session = state.nextAppointment

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        item {
            HomeHeader(navController, session)
            Spacer(modifier = Modifier.height(24.dp))
            NextConsultationCard(navController, session)
            Spacer(modifier = Modifier.height(24.dp))
            BookConsultationButton(navController)
            Spacer(modifier = Modifier.height(32.dp))
            BrowseBySpecialty(navController)
            Spacer(modifier = Modifier.height(32.dp))
            MostOpenSlots(state.doctorsWithOpenSlots)
        }
    }
}

@Composable
private fun HomeHeader(navController: NavController, nextAppointment: LoadState<Appointment?>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Good day", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(
            onClick = { navController.navigate(Screen.Notifications.route) },
            modifier = Modifier
                .clip(CircleShape)
                .background(MediQSurface)
        ) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MediQGreen)
        }
    }
}

@Composable
private fun NextConsultationCard(navController: NavController, state: LoadState<Appointment?>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MediQSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Your next consultation", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is LoadState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MediQGreen)
                    }
                }

                is LoadState.Error -> {
                    Text(text = state.message, color = Color(0xFFD32F2F), fontSize = 14.sp)
                }

                is LoadState.Success -> {
                    val appointment = state.data
                    if (appointment == null) {
                        EmptyStateInline(
                            title = "Nothing booked",
                            description = "Your next appointment will show here once you book a consultation.",
                            modifier = Modifier.background(Color.Transparent)
                        )
                    } else {
                        AppointmentSummary(appointment)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Manage appointment >",
                color = MediQGreen,
                modifier = Modifier
                    .clickable { navController.navigate(Screen.Appointments.route) }
            )
        }
    }
}

@Composable
private fun AppointmentSummary(appointment: Appointment) {
    val date = appointment.startsAt.toClinicDate()
    val time = appointment.startsAt.toClinicTime()

    Column {
        Text(text = time.format(TIME_FORMAT), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MediQGreen)
        Text(text = date.format(DATE_FORMAT), color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = appointment.doctor.displayName, fontWeight = FontWeight.Bold)
        Text(text = appointment.doctor.specialty.displayName, color = Color.Gray)
        Text(
            text = "${appointment.location.building} — ${appointment.location.floor} — ${appointment.location.room}",
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Arrive 15 minutes early for registration", color = MediQGreen, fontSize = 12.sp)
    }
}

@Composable
private fun BookConsultationButton(navController: NavController) {
    Button(
        onClick = { navController.navigate(Screen.Doctors.route) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MediQGreen),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Outlined.MedicalServices, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = "Book a consultation", fontSize = 18.sp)
    }
}

@Composable
private fun BrowseBySpecialty(navController: NavController) {
    Column {
        Text(text = "Browse by specialty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        EmptyStateInline(
            title = "No specialties to browse",
            description = "Specialty listings will appear here once the directory is available.",
            modifier = Modifier.background(Color.Transparent)
        )
    }
}

@Composable
private fun MostOpenSlots(state: LoadState<List<Doctor>>) {
    Column {
        Text(text = "Most open slots this week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        when (state) {
            is LoadState.Loading -> CircularProgressIndicator(color = MediQGreen)

            is LoadState.Error -> Text(text = state.message, color = Color(0xFFD32F2F), fontSize = 14.sp)

            is LoadState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "No availability yet",
                        description = "Doctors with open slots this week will be listed here.",
                    )
                } else {
                    state.data.forEach { doctor ->
                        DoctorSlotRow(doctor)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorSlotRow(doctor: Doctor) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MediQLightGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = doctor.initials, color = MediQGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = doctor.displayName, fontWeight = FontWeight.Bold)
            Text(
                text = "${doctor.specialty.displayName} — ${doctor.location.building} — ${doctor.location.floor}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}