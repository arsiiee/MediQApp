package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailsScreen(navController: NavController, doctorId: String?) {
    var selectedDate by remember { mutableStateOf(0) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MediQLightGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "MS", color = MediQGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Dr. Maria Elena Sandoval", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(text = "Internal Medicine", color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text(text = "Main Building — 2F — Clinic 204", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBox("Experience", "14 years")
                InfoBox("Consultation", "₱700")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Adult internal medicine with focus on diabetes, hypertension, and preventive check-ups. Consults in Filipino, Cebuano, and English.", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Clinic hours", fontWeight = FontWeight.Bold)
            ClinicHourRow("Monday", "9:00 AM – 12:00 PM")
            ClinicHourRow("Wednesday", "9:00 AM – 12:00 PM")
            ClinicHourRow("Friday", "1:00 PM – 4:00 PM")
            Text(text = "License no. PRC 0112443", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Choose a time slot", fontWeight = FontWeight.Bold)
            Text(text = "Reserved and blocked slots cannot be selected — availability updates in real time.", color = Color.Gray, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            DateRow(selectedDate) { selectedDate = it }
            
            Spacer(modifier = Modifier.height(16.dp))
            TimeSlotGrid(selectedTime) { selectedTime = it }
            
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { if (selectedTime != null) navController.navigate(Screen.BookingFlow.createRoute(doctorId ?: "1")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedTime != null) MediQGreen else Color.LightGray),
                enabled = selectedTime != null
            ) {
                Text(text = if (selectedTime != null) "Continue: $selectedTime" else "Select a slot to continue", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun InfoBox(label: String, value: String) {
    Surface(
        color = MediQSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(120.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontWeight = FontWeight.Bold, color = MediQGreen)
        }
    }
}

@Composable
fun ClinicHourRow(day: String, time: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = day, color = Color.Gray)
        Text(text = time, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DateRow(selected: Int, onSelect: (Int) -> Unit) {
    val dates = listOf(21, 23, 25, 28, 30)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        dates.forEachIndexed { index, date ->
            Surface(
                modifier = Modifier
                    .size(width = 50.dp, height = 60.dp)
                    .clickable { onSelect(index) },
                color = if (selected == index) MediQGreen else MediQSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(text = "Sep", color = if (selected == index) Color.White else Color.Gray, fontSize = 12.sp)
                    Text(text = "$date", color = if (selected == index) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TimeSlotGrid(selected: String?, onSelect: (String) -> Unit) {
    val times = listOf("9:00 AM", "9:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM")
    val blocked = listOf("9:30 AM")
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(100.dp)
    ) {
        items(times) { time ->
            val isBlocked = time in blocked
            Surface(
                modifier = Modifier
                    .height(40.dp)
                    .clickable(enabled = !isBlocked) { onSelect(time) },
                color = when {
                    selected == time -> MediQGreen
                    isBlocked -> Color.LightGray.copy(alpha = 0.3f)
                    else -> MediQLightGreen
                },
                shape = RoundedCornerShape(8.dp),
                border = if (selected == time) null else androidx.compose.foundation.BorderStroke(1.dp, MediQGreen.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = time,
                        color = when {
                            selected == time -> Color.White
                            isBlocked -> Color.Gray
                            else -> MediQGreen
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
