package com.example.mediq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Map
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
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailsScreen(
    navController: NavController,
    appointmentId: String?
) {

    var showCancelSheet by remember {
        mutableStateOf(false)
    }

    // -------------------------------------------------
    // SECRETARY MAPPING
    // -------------------------------------------------
    // Mock frontend mapping for now.
    // Later this will come from the backend/database.

    val conversationId = when (appointmentId) {

        "1" -> "maria_secretary"

        "2" -> "kathleen_secretary"

        "3" -> "joel_secretary"

        else -> "maria_secretary"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
    ) {

        // =================================================
        // HEADER
        // =================================================

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
                text = "Appointment Details",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // =================================================
        // CONTENT
        // =================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp)
        ) {

            // -------------------------------------------------
            // CURRENT STATUS
            // -------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Current Status",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MediQTextPrimary
                )

                Text(
                    text = "Confirmed",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MediQGreen
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // =================================================
            // APPOINTMENT INFORMATION CARD
            // =================================================

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFDCE3DE)
                )
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    // Doctor
                    Text(
                        text = "Doctor",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQTextSecondary
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Dr. Maria Elena Sandoval",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediQTextPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = "Internal Medicine",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQGreen
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    HorizontalDivider(
                        color = Color(0xFFE0E5E2)
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    // Schedule
                    Text(
                        text = "Schedule",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQTextSecondary
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Monday, Sep 14, 2026 at 9:30 AM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQTextPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    HorizontalDivider(
                        color = Color(0xFFE0E5E2)
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    // Location
                    Text(
                        text = "Location",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQTextSecondary
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Main Building - 2F · Clinic 204",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQTextPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    HorizontalDivider(
                        color = Color(0xFFE0E5E2)
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    // Reason
                    Text(
                        text = "Reason for visit",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQTextSecondary
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Follow-up for elevated blood pressure",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQTextSecondary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // =================================================
            // MAP PREVIEW
            // =================================================

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE2F2E7)
            ) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = "Map",
                        tint = MediQGreen,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "ACE Medical Center Map Preview",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MediQGreen
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // =================================================
            // CHAT WITH SECRETARY
            // =================================================

            Button(
                onClick = {

                    navController.navigate(
                        Screen.MessageChat.createRoute(
                            conversationId
                        )
                    )

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQGreen
                )
            ) {

                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Chat with Secretary",
                    modifier = Modifier.size(19.dp)
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text = "Chat with Secretary",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            // =================================================
            // CANCEL
            // =================================================

            OutlinedButton(
                onClick = {
                    showCancelSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFF2B8B5)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFFFF7F6),
                    contentColor = Color(0xFFB42318)
                )
            ) {

                Text(
                    text = "Cancel",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // =====================================================
    // CANCEL BOTTOM SHEET
    // =====================================================

    if (showCancelSheet) {

        ModalBottomSheet(
            onDismissRequest = {
                showCancelSheet = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp
            ),
            dragHandle = null
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 4.dp,
                        bottom = 28.dp
                    )
            ) {

                // -------------------------------------------------
                // SHEET HEADER
                // -------------------------------------------------

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentAlignment = Alignment.CenterStart
                ) {

                    Text(
                        text = "Cancel this appointment?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediQTextPrimary
                    )

                    TextButton(
                        onClick = {
                            showCancelSheet = false
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {

                        Text(
                            text = "×",
                            fontSize = 20.sp,
                            color = MediQTextSecondary
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "The reserved slot will be released immediately so another patient can book it.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MediQTextSecondary
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                // -------------------------------------------------
                // CONFIRM CANCELLATION
                // -------------------------------------------------

                Button(
                    onClick = {

                        val id = appointmentId

                        if (!id.isNullOrBlank()) {

                            navController
                                .previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(
                                    "cancelled_appointment_id",
                                    id
                                )
                        }

                        showCancelSheet = false

                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFF3F2),
                        contentColor = Color(0xFFB42318)
                    )
                ) {

                    Text(
                        text = "Cancel Appointment",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }
        }
    }
}