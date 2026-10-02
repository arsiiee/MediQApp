package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NotificationItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Appointment confirmed",
                    description = "Your consultation with Dr. Maria Elena Sandoval has been confirmed for Monday, Sep 14.",
                    time = "1 min ago",
                    iconTint = MediQGreen
                )
            }
            item {
                NotificationItem(
                    icon = Icons.Default.CalendarToday,
                    title = "Reminder: Tomorrow",
                    description = "Please arrive 15 minutes early for your appointment with Dr. Sandoval tomorrow at 9:30 AM.",
                    time = "1 hour ago",
                    iconTint = Color(0xFF1976D2)
                )
            }
            item {
                NotificationItem(
                    icon = Icons.Default.Info,
                    title = "Reschedule approved",
                    description = "Your request to reschedule Dr. Kathleen Lim's slot has been approved for Thursday, Sep 17.",
                    time = "2 hours ago",
                    iconTint = Color(0xFFF57C00)
                )
            }
            item {
                NotificationItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Appointment cancelled",
                    description = "Your appointment with Dr. Ramon Dela Cruz on Sep 11 has been cancelled.",
                    time = "1 day ago",
                    iconTint = Color(0xFFD32F2F)
                )
            }
        }
    }
}

@Composable
fun NotificationItem(icon: ImageVector, title: String, description: String, time: String, iconTint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MediQSurface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = time, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = Color.Gray, fontSize = 14.sp)
        }
    }
}
