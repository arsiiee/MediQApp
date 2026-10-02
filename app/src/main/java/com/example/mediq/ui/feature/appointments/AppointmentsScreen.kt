package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
            .padding(16.dp)
    ) {
        Text(text = "My appointments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MediQGreen,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MediQGreen
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = title) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedTab == 0) {
            UpcomingAppointments(navController)
        } else {
            HistoryAppointments(navController)
        }
    }
}

@Composable
fun UpcomingAppointments(navController: NavController) {
    val appointments = listOf(
        AppointmentData("Dr. Maria Elena Sandoval", "Internal Medicine", "Mon, Sep 14 · 9:30 AM", "Confirmed"),
        AppointmentData("Dr. Kathleen Lim", "Dermatology", "Thu, Sep 17 · 10:00 AM", "Awaiting confirmation")
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
        AppointmentData("Dr. Ramon Dela Cruz", "Orthopedics", "Mon, Aug 24 · 2:00 PM", "Completed"),
        AppointmentData("Dr. Grace Villanueva", "Ob-Gynecology", "Wed, Aug 12 · 11:30 AM", "Completed")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(appointments) { appt ->
            AppointmentItem(appt, onClick = { navController.navigate(Screen.AppointmentDetails.createRoute("1")) })
        }
    }
}

data class AppointmentData(val doctor: String, val specialty: String, val time: String, val status: String)

@Composable
fun AppointmentItem(appointment: AppointmentData, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MediQLightGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = appointment.doctor.split(" ").last().take(1), color = MediQGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = appointment.doctor, fontWeight = FontWeight.Bold)
                Text(text = appointment.specialty, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = appointment.time, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = when (appointment.status) {
                        "Confirmed" -> Color(0xFFE3F2FD)
                        "Completed" -> Color(0xFFE8F5E9)
                        else -> Color(0xFFFFF3E0)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = appointment.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (appointment.status) {
                            "Confirmed" -> Color(0xFF1976D2)
                            "Completed" -> Color(0xFF388E3C)
                            else -> Color(0xFFF57C00)
                        },
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MediQSurface),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = if (appointment.status == "Completed") "View details" else "Manage", color = MediQGreen, fontSize = 12.sp)
                }
            }
        }
    }
}
