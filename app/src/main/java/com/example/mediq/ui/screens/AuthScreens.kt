package com.example.mediq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.R
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.mediq.ui.theme.MediQLightGreen
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check

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

    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.White)
    ) {

        // Hospital header image + logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hospital_image),
                contentDescription = "Hospital",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp),
                contentScale = ContentScale.Crop
            )

            Image(
                painter = painterResource(id = R.drawable.mediq_logo),
                contentDescription = "Hospital Logo",
                modifier = Modifier
                    .size(112.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 40.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {

            Spacer(modifier = Modifier.height(42.dp))

            Text(
                text = "Sign in to book consultations and manage your " +
                        "schedule, or monitor the out-patient appointment system.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MediQTextSecondary,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Username
            Text(
                text = "Username",
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "e.g. maria.santos",
                        color = Color(0xFFA0A8B5)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            Text(
                text = "Password",
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Enter your password",
                        color = Color(0xFFA0A8B5)
                    )
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.Visibility
                            } else {
                                Icons.Filled.VisibilityOff
                            },
                            contentDescription = "Toggle password visibility",
                            tint = MediQTextSecondary
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Forgot password + secure session
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = {
                        // Forgot password will be implemented later
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = MediQGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Secure session",
                        tint = MediQTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "Secure session",
                        color = MediQTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Login
            Button(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignIn.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQGreen
                )
            ) {
                Text(
                    text = "Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Create account
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.RegisterDetails.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFD8DED9)
                )
            ) {
                Text(
                    text = "Create Account / Register",
                    color = MediQGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RegistrationProgress(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (index < currentStep) {
                            MediQGreen
                        } else {
                            Color(0xFFDCE2DE)
                        }
                    )
            )
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
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        // Top app bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MediQGreen
                )
            }

            Text(
                text = "Create your account",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalDivider(
            color = Color(0xFFE5E8E5)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // Registration progress: Step 1 of 3
            RegistrationProgress(currentStep = 1)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "We only collect what is needed to schedule your consultations.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Full Name
            Text(
                text = "Full Name",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Kissie Ann Apple",
                        color = Color(0xFFA0A8B5)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Number
            Text(
                text = "Phone Number",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "+63 917 555 0142",
                        color = Color(0xFFA0A8B5)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Used for the verification code and appointment reminders.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    navController.navigate(Screen.RegisterOTP.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQGreen
                )
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RegisterOTPScreen(navController: NavController) {
    var otp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        // Top app bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MediQGreen
                )
            }

            Text(
                text = "Create your account",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalDivider(
            color = Color(0xFFE5E8E5)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // Step 2 of 3
            RegistrationProgress(currentStep = 2)

            Spacer(modifier = Modifier.height(20.dp))

            // OTP icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MediQLightGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Verification",
                    tint = MediQGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Verify your number",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "We sent a 6-digit code to 093620008050",
                fontSize = 14.sp,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "One-time code",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = otp,
                onValueChange = {
                    if (it.length <= 6 && it.all(Char::isDigit)) {
                        otp = it
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Resend OTP in 25s",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    navController.navigate(Screen.RegisterCredentials.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQGreen
                )
            ) {
                Text(
                    text = "Verify",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
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
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        // Top app bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MediQGreen
                )
            }

            Text(
                text = "Create your account",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalDivider(
            color = Color(0xFFE5E8E5)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            // Step 3 of 3
            RegistrationProgress(currentStep = 3)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Account credentials",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Choose the username and password you will use to sign in.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Username
            Text(
                text = "Username",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            Text(
                text = "Password",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "At least 8 characters. Stored securely as a hash.",
                fontSize = 12.sp,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            Text(
                text = "Confirm Password",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account
            Button(
                onClick = {
                    navController.navigate(Screen.RegisterSuccess.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQGreen
                )
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RegisterSuccessScreen(navController: NavController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // Top app bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MediQGreen
                )
            }

            Text(
                text = "Create your account",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalDivider(
            color = Color(0xFFE5E8E5)
        )

        // Success content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Success icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MediQLightGreen),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = MediQGreen,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Success",
                        tint = MediQGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Account created",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Welcome to MediQ, Kissie. Your account is verified " +
                        "and ready — sign in to search doctors and book " +
                        "your first consultation.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.RegisterDetails.route) {
                            inclusive = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQGreen
                )
            ) {
                Text(
                    text = "Continue to Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
