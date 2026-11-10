package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var attemptedSave by remember { mutableStateOf(false) }

    // Validation checks
    val isCurrentValid = currentPassword == "correctpassword" || (attemptedSave && currentPassword.isNotEmpty())
    val isLengthValid = newPassword.length >= 8
    val doPasswordsMatch = newPassword.isNotEmpty() && newPassword == confirmPassword
    val isFormValid = currentPassword.isNotEmpty() && isLengthValid && doPasswordsMatch

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
                Text(text = "Change Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp)
        ) {
            Text(text = "Update your password to keep your account secure.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))

            // Current Password
            Text(text = "Current Password", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter current password", color = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                        Icon(
                            imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (attemptedSave && currentPassword.isEmpty()) Color(0xFFD32F2F) else MediQGreen,
                    unfocusedBorderColor = if (attemptedSave && currentPassword.isEmpty()) Color(0xFFD32F2F) else Color.LightGray
                )
            )
            if (attemptedSave && currentPassword.isEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Current password is required.", color = Color(0xFFD32F2F), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // New Password
            Text(text = "New Password", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter new password", color = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (attemptedSave && !isLengthValid) Color(0xFFD32F2F) else MediQGreen,
                    unfocusedBorderColor = if (attemptedSave && !isLengthValid) Color(0xFFD32F2F) else Color.LightGray
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isLengthValid) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "At least 8 characters", color = Color(0xFF2E7D32), fontSize = 12.sp)
                }
            } else if (attemptedSave) {
                Text(text = "Password is too short. Use at least 8 characters.", color = Color(0xFFD32F2F), fontSize = 12.sp)
            } else {
                Text(text = "At least 8 characters", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm New Password
            Text(text = "Confirm New Password", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Re-enter new password", color = Color.Gray) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (attemptedSave && !doPasswordsMatch) Color(0xFFD32F2F) else MediQGreen,
                    unfocusedBorderColor = if (attemptedSave && !doPasswordsMatch) Color(0xFFD32F2F) else Color.LightGray
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (doPasswordsMatch) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Passwords match", color = Color(0xFF2E7D32), fontSize = 12.sp)
                }
            } else if (attemptedSave) {
                Text(text = "Passwords don't match. Re-enter your new password.", color = Color(0xFFD32F2F), fontSize = 12.sp)
            } else {
                Text(text = "Must match your new password", color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    attemptedSave = true
                    if (isFormValid) {
                        navController.popBackStack()
                    }
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
        }
    }
}
