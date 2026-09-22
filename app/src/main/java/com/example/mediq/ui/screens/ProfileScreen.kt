package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQSurface

@Composable
fun ProfileScreen(navController: NavController) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MediQLightGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "AZ", color = MediQGreen, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Arweyne Zoe Salcedo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = "zoe.salcedo@email.com", color = Color.Gray)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MediQSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileInfoItem("Mobile number", "0917 442 8810")
                    ProfileInfoItem("Date of birth", "1990-04-12")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileInfoItem("Sex", "Female")
                    ProfileInfoItem("Address", "Zone 4, Carmen, Cagayan de Oro City")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MediQGreen)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MediQGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit my details", color = MediQGreen)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ProfileOption(Icons.Default.Notifications, "Notifications", "2 unread", onClick = { navController.navigate(Screen.Notifications.route) })
        Spacer(modifier = Modifier.height(12.dp))
        ProfileOption(Icons.Default.Logout, "Sign out", null, isCritical = true, onClick = { showSignOutDialog = true })
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("Are you sure you want to sign out of your MediQ account?") },
            confirmButton = {
                Button(
                    onClick = { 
                        showSignOutDialog = false
                        navController.navigate(Screen.Splash.route) {
                            popUpTo(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProfileOption(icon: ImageVector, title: String, trailingText: String?, isCritical: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MediQSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isCritical) Color(0xFFD32F2F) else MediQGreen)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, modifier = Modifier.weight(1f), color = if (isCritical) Color(0xFFD32F2F) else Color.Black)
            if (trailingText != null) {
                Surface(color = Color(0xFFFFF3E0), shape = CircleShape) {
                    Text(text = trailingText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFFE65100))
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
