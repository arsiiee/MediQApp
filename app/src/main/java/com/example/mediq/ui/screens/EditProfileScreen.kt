package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController, passwordChanged: Boolean = false) {
    var fullName by remember { mutableStateOf(UserProfile.fullName) }
    var phoneNumber by remember { mutableStateOf(UserProfile.phoneNumber) }
    var address by remember { mutableStateOf(UserProfile.address) }
    var sex by remember { mutableStateOf(UserProfile.sex) }
    var dateOfBirth by remember { mutableStateOf(UserProfile.dateOfBirth) }
    var showSuccessBanner by remember { mutableStateOf(passwordChanged) }

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
                Text(text = "Edit My Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            if (showSuccessBanner) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Password changed successfully.", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Personal Information Section
            Text(text = "Personal Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Full Name", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color.LightGray
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Phone Number", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color.LightGray
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Sex", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Female", "Male").forEach { option ->
                    val isSelected = sex == option
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { sex = option },
                        color = if (isSelected) MediQGreen else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MediQGreen else Color.LightGray
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = option,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Date of Birth", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color.LightGray
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Address", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color.LightGray
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    UserProfile.fullName = fullName
                    UserProfile.phoneNumber = phoneNumber
                    UserProfile.address = address
                    UserProfile.sex = sex
                    UserProfile.dateOfBirth = dateOfBirth
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediQGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C))
            ) {
                Text(text = "Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))

            // Change Password Section
            Text(text = "Change Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Keep your account secure by updating your password.", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { navController.navigate(Screen.ChangePassword.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Change Password", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
