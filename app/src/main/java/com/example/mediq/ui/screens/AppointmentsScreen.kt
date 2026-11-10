package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQSurface

@Composable
fun AppointmentsScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "History")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Green Header (Equal Height: 64.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MediQGreen)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = "My Appointments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            // Pill Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = selectedTab == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { selectedTab = index },
                        color = if (selected) MediQGreen else MediQSurface,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = title,
                                color = if (selected) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (selectedTab == 0) {
                UpcomingAppointments(navController)
            } else {
                HistoryAppointments(navController)
            }
        }
    }
}

@Composable
fun UpcomingAppointments(navController: NavController) {
    val appointments = listOf(
        AppointmentData("Dr. Maria Elena Sandoval", "Internal Medicine", "Mon, Sep 14 · 9:30 AM", "Main Building — 2F — Clinic 204", "Follow-up for elevated blood pressure", "Confirmed"),
        AppointmentData("Dr. Kathleen Lim", "Dermatology", "Thu, Sep 17 · 10:00 AM", "Annex Wing — 1F — Clinic 114", "Recurring skin rash on both arms", "Awaiting confirmation")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(appointments) { appt ->
            AppointmentItem(appt, onClick = { navController.navigate(Screen.AppointmentDetails.createRoute("1")) })
        }
    }
}

@Composable
fun HistoryAppointments(navController: NavController) {
    val appointments = listOf(
        AppointmentData("Dr. Ramon Dela Cruz", "Orthopedics", "Mon, Aug 24 · 2:00 PM", "Main Building — 3F — Clinic 312", "Knee joint pain after workout", "Completed"),
        AppointmentData("Dr. Grace Villanueva", "Ob-Gynecology", "Wed, Aug 12 · 11:30 AM", "Annex Wing — 4F — Clinic 402", "Annual routine check-up", "Completed")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(appointments) { appt ->
            AppointmentItem(appt, onClick = { navController.navigate(Screen.AppointmentDetails.createRoute("1")) })
        }
    }
}

data class AppointmentData(
    val doctor: String,
    val specialty: String,
    val time: String,
    val location: String,
    val reason: String,
    val status: String
)

@Composable
fun AppointmentItem(appointment: AppointmentData, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MediQLightGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appointment.doctor.split(" ").let { it.firstOrNull()?.take(1) + (it.getOrNull(1)?.take(1) ?: "") },
                            color = MediQGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = appointment.doctor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = appointment.specialty, color = Color.Gray, fontSize = 13.sp)
                    }
                }

                Surface(
                    color = when (appointment.status) {
                        "Confirmed" -> Color(0xFFE8F5E9)
                        "Completed" -> Color(0xFFE8F5E9)
                        else -> Color(0xFFFFF3E0)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = appointment.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = when (appointment.status) {
                            "Confirmed" -> Color(0xFF2E7D32)
                            "Completed" -> Color(0xFF2E7D32)
                            else -> Color(0xFFE65100)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = appointment.time, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = appointment.location, fontSize = 13.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = MediQSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reason: ${appointment.reason}",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}
