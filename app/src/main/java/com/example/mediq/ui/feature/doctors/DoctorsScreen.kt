package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorsScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(text = "Search doctor name or specialty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = Color.LightGray
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("Filter by specialty") })
            FilterChip(selected = false, onClick = {}, label = { Text("Filter by clinic location") })
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        val doctors = listOf(
            DoctorInfo("Dr. Maria Elena Sandoval", "Internal Medicine", 14, 700, "Main Building — 2F — Clinic 204"),
            DoctorInfo("Dr. Joel Marquez", "Pediatrics", 18, 500, "Annex Wing — 1F — Clinic 106"),
            DoctorInfo("Dr. Antonio Reyes Jr.", "Cardiology", 20, 1000, "Annex Wing — 3F — Heart Station 301"),
            DoctorInfo("Dr. Grace Villanueva", "Ob-Gynecology", 15, 800, "Annex Wing — 4F — Clinic 402"),
            DoctorInfo("Dr. Ramon Dela Cruz", "Orthopedics", 13, 900, "Main Building — 3F — Clinic 312")
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(doctors) { doctor ->
                DoctorListItem(doctor, onClick = { navController.navigate(Screen.DoctorDetails.createRoute("1")) })
            }
        }
    }
}

data class DoctorInfo(val name: String, val specialty: String, val experience: Int, val fee: Int, val location: String)

@Composable
fun DoctorListItem(doctor: DoctorInfo, onClick: () -> Unit) {
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
                    .size(64.dp)
                    .background(MediQLightGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = doctor.name.split(" ").last().take(1) + doctor.name.split(" ").drop(1).firstOrNull()?.take(1), 
                    color = MediQGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = doctor.name, fontWeight = FontWeight.Bold)
                Text(text = doctor.specialty, color = Color.Gray, fontSize = 14.sp)
                Text(text = doctor.location, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Experience", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${doctor.experience} years", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Consultation", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "₱${doctor.fee}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
