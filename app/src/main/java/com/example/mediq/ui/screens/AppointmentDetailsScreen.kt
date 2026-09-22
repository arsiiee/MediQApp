package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailsScreen(navController: NavController, appointmentId: String?) {
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointment Details", fontWeight = FontWeight.Bold) },
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Current Status", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Confirmed",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF1976D2),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Doctor", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = "Dr. Maria Elena Sandoval", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "Internal Medicine", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Schedule", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = "Monday, Sep 14, 2026 at 9:30 AM", fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Location", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = "Main Building — 2F — Clinic 204", fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Reason for visit", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = "Follow-up for elevated blood pressure", fontWeight = FontWeight.Medium)
            
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = { /* Request Reschedule */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MediQGreen)
            ) {
                Text(text = "Request Reschedule", color = MediQGreen, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { showCancelDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Cancel Appointment", color = Color.Gray)
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel this appointment?") },
            text = { Text("The reserved slot will be released immediately so another patient can book it.") },
            confirmButton = {
                Button(
                    onClick = { 
                        showCancelDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Cancel Appointment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Back", color = Color.Gray)
                }
            }
        )
    }
}
