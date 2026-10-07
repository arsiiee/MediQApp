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

private data class AppointmentDetailsData(
    val doctor: String,
    val specialty: String,
    val date: String,
    val time: String,
    val location: String,
    val reason: String,
    val status: String
)

private fun getAppointmentDetails(
    appointmentId: String?
): AppointmentDetailsData {

    return when (appointmentId) {

        // =================================================
        // UPCOMING - MARIA
        // =================================================

        "1" -> AppointmentDetailsData(
            doctor = "Dr. Maria Elena Sandoval",
            specialty = "Internal Medicine",
            date = "Monday, September 14, 2026",
            time = "9:30 AM",
            location = "Main Building — 2F · Clinic 204",
            reason = "Follow-up for elevated blood pressure",
            status = "Confirmed"
        )

        // =================================================
        // UPCOMING - KATHLEEN
        // =================================================

        "2" -> AppointmentDetailsData(
            doctor = "Dr. Kathleen Lim",
            specialty = "Dermatology",
            date = "Thursday, September 17, 2026",
            time = "10:00 AM",
            location = "Annex Wing — 1F · Clinic 114",
            reason = "Recurring skin rash on both arms",
            status = "Awaiting confirmation"
        )

        // =================================================
        // HISTORY - RAMON
        // =================================================

        "3" -> AppointmentDetailsData(
            doctor = "Dr. Ramon Dela Cruz",
            specialty = "Orthopedics",
            date = "Monday, August 24, 2026",
            time = "2:00 PM",
            location = "Main Building — 2F · Clinic 212",
            reason = "Knee pain consultation",
            status = "Completed"
        )

        // =================================================
        // HISTORY - GRACE
        // =================================================

        "4" -> AppointmentDetailsData(
            doctor = "Dr. Grace Villanueva",
            specialty = "OB-Gynecology",
            date = "Wednesday, August 12, 2026",
            time = "11:30 AM",
            location = "Annex Wing — 4F · Clinic 402",
            reason = "Routine consultation",
            status = "Completed"
        )

        // =================================================
        // FALLBACK
        // =================================================

        else -> AppointmentDetailsData(
            doctor = "Dr. Maria Elena Sandoval",
            specialty = "Internal Medicine",
            date = "Monday, September 14, 2026",
            time = "9:30 AM",
            location = "Main Building — 2F · Clinic 204",
            reason = "Follow-up for elevated blood pressure",
            status = "Confirmed"
        )
    }
}

private fun getConversationId(
    appointmentId: String?
): String {

    return when (appointmentId) {

        "1" -> "maria_secretary"
        "2" -> "kathleen_secretary"
        "3" -> "joel_secretary"

        // Grace currently uses a safe fallback because
        // there is no separate Grace secretary conversation.
        "4" -> "maria_secretary"

        else -> "maria_secretary"
    }
}

private fun canCancelAppointment(
    status: String
): Boolean {

    return status == "Confirmed" ||
            status == "Awaiting confirmation"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailsScreen(
    navController: NavController,
    appointmentId: String?
) {

    var showCancelSheet by remember {
        mutableStateOf(false)
    }

    val appointment =
        getAppointmentDetails(appointmentId)

    val conversationId =
        getConversationId(appointmentId)

    val canCancel =
        canCancelAppointment(
            appointment.status
        )

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
                    /*
                     * IMPORTANT:
                     * Do NOT navigate to Screen.Appointments here.
                     *
                     * popBackStack() returns to the exact
                     * AppointmentsScreen instance that opened this
                     * appointment.
                     *
                     * AppointmentsScreen already knows whether
                     * the appointment came from Upcoming or History.
                     */
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

            // =================================================
            // CURRENT STATUS
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Current Status",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MediQTextPrimary
                )

                Text(
                    text = appointment.status,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,

                    color = when (appointment.status) {

                        "Cancelled" ->
                            Color(0xFFB42318)

                        "Completed" ->
                            MediQGreen

                        "Awaiting confirmation" ->
                            Color(0xFFB76E00)

                        else ->
                            MediQGreen
                    }
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

                shape =
                    RoundedCornerShape(14.dp),

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
                        text = appointment.doctor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediQTextPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = appointment.specialty,
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
                        text =
                            "${appointment.date} at ${appointment.time}",
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
                        text = appointment.location,
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
                        text = appointment.reason,
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

                shape =
                    RoundedCornerShape(12.dp),

                color = Color(0xFFE2F2E7)
            ) {

                Column(
                    modifier = Modifier.fillMaxSize(),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Map,

                        contentDescription =
                            "Map",

                        tint = MediQGreen,

                        modifier =
                            Modifier.size(24.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            "ACE Medical Center Map Preview",

                        fontSize = 11.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color =
                            MediQGreen
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

                shape =
                    RoundedCornerShape(11.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            MediQGreen
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.ChatBubbleOutline,

                    contentDescription =
                        "Chat with Secretary",

                    modifier =
                        Modifier.size(19.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(7.dp)
                )

                Text(
                    text =
                        "Chat with Secretary",

                    fontSize = 13.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            // =================================================
            // CANCEL
            // =================================================

            if (canCancel) {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedButton(

                    onClick = {
                        showCancelSheet = true
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),

                    shape =
                        RoundedCornerShape(11.dp),

                    border =
                        BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFF2B8B5)
                        ),

                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                containerColor =
                                    Color(0xFFFFF7F6),

                                contentColor =
                                    Color(0xFFB42318)
                            )
                ) {

                    Text(
                        text =
                            "Cancel",

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
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

            containerColor =
                Color.White,

            shape =
                RoundedCornerShape(
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

                // Sheet header

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),

                    contentAlignment =
                        Alignment.CenterStart
                ) {

                    Text(
                        text =
                            "Cancel this appointment?",

                        fontSize = 17.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MediQTextPrimary
                    )

                    TextButton(

                        onClick = {
                            showCancelSheet = false
                        },

                        modifier = Modifier
                            .align(
                                Alignment.CenterEnd
                            )
                            .size(36.dp),

                        contentPadding =
                            PaddingValues(0.dp)
                    ) {

                        Text(
                            text = "×",
                            fontSize = 20.sp,
                            color = MediQTextSecondary
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "The reserved slot will be released immediately so another patient can book it.",

                    fontSize = 13.sp,

                    lineHeight = 19.sp,

                    color =
                        MediQTextSecondary
                )

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                // Confirm cancellation

                Button(

                    onClick = {

                        val id =
                            appointmentId

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

                    shape =
                        RoundedCornerShape(11.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFFFFF3F2),

                            contentColor =
                                Color(0xFFB42318)
                        )
                ) {

                    Text(
                        text =
                            "Cancel Appointment",

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )
            }
        }
    }
}