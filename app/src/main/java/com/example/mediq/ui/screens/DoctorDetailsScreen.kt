package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun DoctorDetailsScreen(navController: NavController, doctorId: String?) {
    var selectedDate by remember { mutableIntStateOf(0) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Doctors Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Scrollable Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // Doctor Info Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MediQLightGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "ME", color = MediQGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Dr. Maria Elena Sandoval", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Internal Medicine", color = MediQGreen, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Main Building — 2F · Clinic 204", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                // Experience & Consultation Boxes
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        color = MediQSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Experience", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "14 years", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        }
                    }
                    Surface(
                        color = MediQSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Consultation", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "₱700", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Adult internal medicine with focus on diabetes, hypertension, and preventive check-ups. Consults in Filipino, Cebuano, and English.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            item {
                // Clinic Hours Card
                Surface(
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Clinic hours", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        ClinicHourRow("Monday", "9:00 AM – 12:00 PM")
                        ClinicHourRow("Wednesday", "9:00 AM – 12:00 PM")
                        ClinicHourRow("Friday", "1:00 PM – 4:00 PM")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "License no. PRC 0112443", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            item {
                // Choose a time slot
                Column {
                    Text(text = "Choose a time slot", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reserved and blocked slots cannot be selected — availability updates in real time.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            item {
                // Date selection row
                val dates = listOf(
                    DateSlot("Mon", 21, "1 open"),
                    DateSlot("Wed", 23, "4 open"),
                    DateSlot("Fri", 25, "2 open"),
                    DateSlot("Mon", 28, "2 open"),
                    DateSlot("Wed", 30, "3 open")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dates.forEachIndexed { index, dateSlot ->
                        val isSelected = selectedDate == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(76.dp)
                                .clickable { selectedDate = index },
                            color = if (isSelected) MediQGreen else Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = dateSlot.day,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${dateSlot.date}",
                                    color = if (isSelected) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = dateSlot.openText,
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else MediQGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            item {
                // Time Slot Grid
                val times = listOf("9:00 AM", "9:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM")
                val blocked = listOf("9:00 AM", "9:30 AM")
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        times.take(3).forEach { time ->
                            TimeSlotButton(
                                time = time,
                                isSelected = selectedTime == time,
                                isBlocked = time in blocked,
                                onClick = { selectedTime = time },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        times.drop(3).forEach { time ->
                            TimeSlotButton(
                                time = time,
                                isSelected = selectedTime == time,
                                isBlocked = time in blocked,
                                onClick = { selectedTime = time },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = MediQGreen, text = "Available")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = Color.Gray, text = "Reserved")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = Color(0xFFD32F2F), text = "Blocked")
                }
            }
        }

        // Bottom Action Button
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { if (selectedTime != null) navController.navigate(Screen.BookingFlow.createRoute(doctorId ?: "1")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTime != null) MediQGreen else MediQSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (selectedTime != null) "Continue · $selectedTime" else "Select a slot to continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTime != null) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

data class DateSlot(val day: String, val date: Int, val openText: String)

@Composable
fun ClinicHourRow(day: String, time: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = day, color = Color.Gray, fontSize = 13.sp)
        Text(text = time, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
fun TimeSlotButton(time: String, isSelected: Boolean, isBlocked: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(enabled = !isBlocked) { onClick() },
        color = when {
            isSelected -> MediQGreen
            isBlocked -> MediQSurface
            else -> MediQSurface
        },
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, if (isBlocked) Color.LightGray.copy(alpha = 0.5f) else MediQGreen.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = time,
                color = when {
                    isSelected -> Color.White
                    isBlocked -> Color.Gray.copy(alpha = 0.6f)
                    else -> MediQGreen
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        ) {
            // Dot
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color.Gray, fontSize = 12.sp)
    }
}
