package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun HomeScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        item {
            HomeHeader(navController)
            Spacer(modifier = Modifier.height(24.dp))
            NextConsultationCard(navController)
            Spacer(modifier = Modifier.height(24.dp))
            BookConsultationButton(navController)
            Spacer(modifier = Modifier.height(32.dp))
            BrowseBySpecialty()
            Spacer(modifier = Modifier.height(32.dp))
            MostOpenSlots(navController)
        }
    }
}

@Composable
fun HomeHeader(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Good day,", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text(text = "Jesse", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
fun NextConsultationCard(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MediQSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Your next consultation", fontWeight = FontWeight.SemiBold)
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Confirmed",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF1976D2),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "9:30 AM", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MediQGreen)
            Text(text = "Monday, September 14, 2026", color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Dr. Maria Elena Sandoval", fontWeight = FontWeight.Bold)
            Text(text = "Internal Medicine", color = Color.Gray)
            Text(text = "Main Building — 2F — Clinic 204", color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Arrive 15 minutes early for registration", color = MediQGreen, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Manage appointment >",
                color = MediQGreen,
                modifier = Modifier.clickable { navController.navigate(Screen.AppointmentDetails.createRoute("1")) }
            )
        }
    }
}

@Composable
fun BookConsultationButton(navController: NavController) {
    Button(
        onClick = { navController.navigate(Screen.Doctors.route) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MediQGreen),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null) // Replace with stethoscope icon
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Book a consultation", fontSize = 18.sp)
    }
}

@Composable
fun BrowseBySpecialty() {
    val specialties = listOf("Internal Medicine", "Pediatrics", "Cardiology", "Dermatology")
    Column {
        Text(text = "Browse by specialty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(specialties) { specialty ->
                Surface(
                    color = MediQSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { }
                ) {
                    Text(
                        text = specialty,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MostOpenSlots(navController: NavController) {
    val doctors = listOf(
        DoctorSlot("Dr. Joel Marquez", "Pediatrics — Annex Wing — 1F", 18),
        DoctorSlot("Dr. Grace Villanueva", "Ob-Gynecology — Annex Wing — 4F", 15),
        DoctorSlot("Dr. Ramon Dela Cruz", "Orthopedics — Main Building — 3F", 13)
    )
    Column {
        Text(text = "Most open slots this week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        doctors.forEach { doctor ->
            DoctorSlotItem(doctor, onClick = { navController.navigate(Screen.DoctorDetails.createRoute("1")) })
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

data class DoctorSlot(val name: String, val info: String, val slots: Int)

@Composable
fun DoctorSlotItem(doctor: DoctorSlot, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MediQLightGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = doctor.name.take(1), color = MediQGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = doctor.name, fontWeight = FontWeight.Bold)
                Text(text = doctor.info, color = Color.Gray, fontSize = 12.sp)
            }
            Text(text = "${doctor.slots}", fontWeight = FontWeight.Bold, color = MediQGreen)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
