package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFlowScreen(navController: NavController, doctorId: String?) {
    var reason by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Appointment", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp)
        ) {
            Text(text = "BOOKING DETAILS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Dr. Maria Elena Sandoval", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Internal Medicine", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Monday, Sep 14, 2026", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "9:30 AM", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MediQGreen)
            Text(text = "Main Building — 2F — Clinic 204", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Reason for visit", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Enter the reason for your consultation...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                Text(text = "I confirm this booking and will arrive 15 minutes early.", style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { navController.navigate(Screen.BookingSuccess.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (confirmed) MediQGreen else Color.LightGray),
                enabled = confirmed
            ) {
                Text(text = "Confirm Booking", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun BookingSuccessScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MediQGreen,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Booking Successful",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dr. Maria Elena Sandoval",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Monday, September 14 — Monday, September 21, 2026 at 10:00 AM. The clinic secretary will confirm your appointment, and you will receive a reminder a day before.",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { 
                    navController.navigate(Screen.Appointments.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
            ) {
                Text("View my appointments", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { /* Add to calendar */ }) {
                Text("Add to Google Calendar", color = MediQGreen)
            }
        }
    }
}
