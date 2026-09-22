package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MediQGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MediQ",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sign in to book consultations and manage your schedule, or check out our patient appointment system.",
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { navController.navigate(Screen.SignIn.route) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp)
            ) {
                Text(text = "Sign in", color = MediQGreen, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { navController.navigate(Screen.RegisterDetails.route) }) {
                Text(text = "Create Account / Register", color = Color.White)
            }
        }
    }
}

@Composable
fun SignInScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "MediQ",
            color = MediQGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Sign in",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your role is detected automatically after authentication.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Username", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. maria.santos") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = MediQGreen
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Password", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor = MediQGreen
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { /* Forgot Password */ }) {
                Text("Forgot Password?", color = MediQGreen)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = false, onCheckedChange = {})
                Text("Secure session", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate(Screen.Home.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
        ) {
            Text("Login", fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { navController.navigate(Screen.RegisterDetails.route) }) {
            Text("Create Account / Register", color = MediQGreen)
        }
    }
}

@Composable
fun RegisterDetailsScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Create your account",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "We only collect what is needed to schedule your consultations.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Full Name", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Maria Santos") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = Color.LightGray
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Phone Number", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("+63 917 555 0142") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = Color.LightGray
            )
        )
        Text(
            text = "Used for the verification code and appointment reminders.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate(Screen.RegisterOTP.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
        ) {
            Text("Continue", fontSize = 18.sp)
        }
    }
}

@Composable
fun RegisterOTPScreen(navController: NavController) {
    var otp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Verify your number",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "We sent a 6-digit code to 09362000858",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("One-time code", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = otp,
            onValueChange = { otp = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Resend OTP in 25s") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = Color.LightGray
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate(Screen.RegisterCredentials.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
        ) {
            Text("Verify", fontSize = 18.sp)
        }
    }
}

@Composable
fun RegisterCredentialsScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Account credentials",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose the username and password you will use to sign in.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Username", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = Color.LightGray
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Password", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = Color.LightGray
            )
        )
        Text(
            text = "At least 8 characters. Stored securely as a hash.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Confirm Password", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MediQGreen,
                unfocusedBorderColor = Color.LightGray
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate(Screen.RegisterSuccess.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
        ) {
            Text("Create Account", fontSize = 18.sp)
        }
    }
}

@Composable
fun RegisterSuccessScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Visibility, // Placeholder for success check
                contentDescription = null,
                tint = MediQGreen,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Account created",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to MediQ, Jesse. Your account is verified and ready — sign in to search doctors and book your first consultation.",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { navController.navigate(Screen.SignIn.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MediQGreen)
            ) {
                Text("Continue to Login", fontSize = 18.sp)
            }
        }
    }
}
