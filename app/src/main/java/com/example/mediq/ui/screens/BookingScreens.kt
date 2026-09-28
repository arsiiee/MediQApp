package com.example.mediq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

@Composable
fun BookingFlowScreen(
    navController: NavController,
    doctorId: String?,
    dateIndex: Int,
    selectedTime: String
) {
    var reason by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }

    // These must match the date order in DoctorDetailsScreen.kt
    val dates = listOf(
        "Monday, September 21, 2026",
        "Wednesday, September 23, 2026",
        "Friday, September 25, 2026",
        "Monday, September 28, 2026",
        "Wednesday, September 30, 2026"
    )

    val selectedDate = dates.getOrElse(dateIndex) {
        dates.first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
    ) {

        // ─────────────────────────────
        // HEADER
        // ─────────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(MediQGreen)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    navController.popBackStack()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Book Appointment",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ─────────────────────────────
        // CONTENT
        // ─────────────────────────────

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(10.dp))

            // ─────────────────────────
            // BOOKING DETAILS
            // ─────────────────────────

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFDCE3DE)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "BOOKING DETAILS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediQTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Dr. Maria Elena Sandoval",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediQTextPrimary
                    )

                    Text(
                        text = "Internal Medicine",
                        fontSize = 13.sp,
                        color = MediQTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        color = Color(0xFFDCE3DE)
                    )

                    Spacer(modifier = Modifier.height(118.dp))

                    Text(
                        text = selectedDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediQTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = selectedTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQGreen
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MediQTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "Main Building — 2F · Clinic 204",
                            fontSize = 12.sp,
                            color = MediQTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─────────────────────────
            // REASON FOR VISIT
            // ─────────────────────────

            Text(
                text = "Reason for visit",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                placeholder = {
                    Text(
                        text = "Enter the reason for your consultation...",
                        color = Color(0xFFA0A9A4),
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ─────────────────────────
            // CONFIRMATION
            // ─────────────────────────

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = confirmed,
                    onCheckedChange = {
                        confirmed = it
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MediQGreen,
                        uncheckedColor = MediQTextSecondary
                    )
                )

                Text(
                    text = "I confirm this booking and will arrive 15 minutes early.",
                    modifier = Modifier.padding(end = 4.dp),
                    fontSize = 12.sp,
                    color = MediQTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ─────────────────────────
            // CONFIRM BOOKING
            // ─────────────────────────

            Button(
                onClick = {
                    navController.navigate(
                        Screen.BookingSuccess.createRoute(
                            dateIndex = dateIndex,
                            time = selectedTime
                        )
                    )
                },
                enabled = confirmed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQGreen,
                    disabledContainerColor = Color(0xFFB7D0BC),
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    text = "Confirm Booking",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun BookingSuccessScreen(
    navController: NavController,
    dateIndex: Int,
    selectedTime: String
) {
    // Same date order used by DoctorDetailsScreen.kt
    val dates = listOf(
        "Monday, September 21, 2026",
        "Wednesday, September 23, 2026",
        "Friday, September 25, 2026",
        "Monday, September 28, 2026",
        "Wednesday, September 30, 2026"
    )

    val selectedDate = dates.getOrElse(dateIndex) {
        dates.first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
    ) {

        // ─────────────────────────────
        // HEADER
        // ─────────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(MediQGreen)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Booking Successful",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ─────────────────────────────
        // SUCCESS CONTENT
        // ─────────────────────────────

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MediQLightGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Booking successful",
                    tint = MediQGreen,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your slot is reserved",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // IMPORTANT:
            // This uses the actual date/time selected by the patient.
            Text(
                text = "Dr. Maria Elena Sandoval · $selectedDate at $selectedTime. " +
                        "The clinic secretary will confirm your appointment, " +
                        "and you will receive a reminder a day before.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ─────────────────────────
            // VIEW APPOINTMENTS
            // ─────────────────────────

            Button(
                onClick = {
                    navController.navigate(Screen.Appointments.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
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
                    text = "View my appointments",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─────────────────────────
            // GOOGLE CALENDAR
            // ─────────────────────────

            OutlinedButton(
                onClick = {
                    // Google Calendar integration will be connected later.
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFD8DED9)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = MediQGreen
                )
            ) {
                Text(
                    text = "Add to Google Calendar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}