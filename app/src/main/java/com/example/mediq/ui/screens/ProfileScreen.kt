package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
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

object UserProfile {
    var fullName by mutableStateOf("Arweyne Zoe Salcedo")
    var email by mutableStateOf("zoe.salcedo@email.com")
    var phoneNumber by mutableStateOf("0917 442 8810")
    var sex by mutableStateOf("Female")
    var dateOfBirth by mutableStateOf("1990-04-12")
    var address by mutableStateOf("Zone 4, Carmen, Cagayan de Oro City")
}

@Composable
fun ProfileScreen(navController: NavController) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    val initials = remember(UserProfile.fullName) {
        UserProfile.fullName.split(" ").let { parts ->
            val first = parts.firstOrNull()?.take(1) ?: ""
            val last = parts.drop(1).lastOrNull()?.take(1) ?: ""
            "$first$last".uppercase()
        }
    }

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
            Text(text = "My Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MediQLightGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = initials, color = MediQGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = UserProfile.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = UserProfile.email, color = Color.Gray, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details Card
            Surface(
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Mobile number", color = Color.Gray, fontSize = 14.sp)
                        Text(text = UserProfile.phoneNumber, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Email", color = Color.Gray, fontSize = 14.sp)
                        Text(text = UserProfile.email, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Sex", color = Color.Gray, fontSize = 14.sp)
                        Text(text = UserProfile.sex, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Date of Birth", color = Color.Gray, fontSize = 14.sp)
                        Text(text = UserProfile.dateOfBirth, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Address", color = Color.Gray, fontSize = 14.sp)
                        Text(text = UserProfile.address, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Edit my details Button
            OutlinedButton(
                onClick = { navController.navigate(Screen.EditProfile.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Edit my details", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Out Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { showSignOutDialog = true },
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFB71C1C), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Sign out", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of your MediQ account?") },
            shape = RoundedCornerShape(16.dp),
            confirmButton = {
                Button(
                    onClick = { 
                        showSignOutDialog = false
                        navController.navigate(Screen.Splash.route) {
                            popUpTo(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSignOutDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF991B1B))
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
