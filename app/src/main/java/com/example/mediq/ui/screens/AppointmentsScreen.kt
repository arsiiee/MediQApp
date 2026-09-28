package com.example.mediq.ui.screens

import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
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
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

@Composable
fun AppointmentsScreen(
    navController: NavController
) {
    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    val upcomingAppointments = remember {
        mutableStateListOf(
            AppointmentData(
                id = "1",
                doctor = "Dr. Maria Elena Sandoval",
                initials = "ME",
                specialty = "Internal Medicine",
                date = "Mon, Sep 14",
                time = "9:30 AM",
                location = "Main Building — 2F · Clinic 204",
                reason = "Follow-up for elevated blood pressure",
                status = "Confirmed"
            ),
            AppointmentData(
                id = "2",
                doctor = "Dr. Kathleen Lim",
                initials = "KL",
                specialty = "Dermatology",
                date = "Thu, Sep 17",
                time = "10:00 AM",
                location = "Annex Wing — 1F · Clinic 114",
                reason = "Recurring skin rash on both arms",
                status = "Awaiting confirmation"
            )
        )
    }

    val historyAppointments = remember {
        mutableStateListOf(
            AppointmentData(
                id = "3",
                doctor = "Dr. Ramon Dela Cruz",
                initials = "RD",
                specialty = "Orthopedics",
                date = "Mon, Aug 24",
                time = "2:00 PM",
                location = "Main Building — 2F · Clinic 212",
                reason = "Knee pain consultation",
                status = "Completed"
            ),
            AppointmentData(
                id = "4",
                doctor = "Dr. Grace Villanueva",
                initials = "GV",
                specialty = "OB-Gynecology",
                date = "Wed, Aug 12",
                time = "11:30 AM",
                location = "Annex Wing — 4F · Clinic 402",
                reason = "Routine consultation",
                status = "Completed"
            )
        )
    }

    // Listen for a cancelled appointment coming back
// from AppointmentDetailsScreen.
    val backStackEntry by navController.currentBackStackEntryAsState()

    val cancelledAppointmentId =
        backStackEntry
            ?.savedStateHandle
            ?.get<String>("cancelled_appointment_id")

    LaunchedEffect(cancelledAppointmentId) {

        if (!cancelledAppointmentId.isNullOrBlank()) {

            val appointmentIndex =
                upcomingAppointments.indexOfFirst {
                    it.id == cancelledAppointmentId
                }

            if (appointmentIndex >= 0) {

                val appointment =
                    upcomingAppointments.removeAt(appointmentIndex)

                historyAppointments.add(
                    appointment.copy(
                        status = "Cancelled"
                    )
                )
            }

            backStackEntry
                ?.savedStateHandle
                ?.remove<String>("cancelled_appointment_id")
        }
    }

    val tabs = listOf(
        "Upcoming",
        "History"
    )

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
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My appointments",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ─────────────────────────────
        // TABS
        // ─────────────────────────────

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            AppointmentTab(
                text = "Upcoming",
                selected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedTab = 0
                }
            )

            AppointmentTab(
                text = "History",
                selected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedTab = 1
                }
            )
        }

        // ─────────────────────────────
        // APPOINTMENTS
        // ─────────────────────────────

        if (selectedTab == 0) {

            AppointmentList(
                appointments = upcomingAppointments,
                navController = navController
            )

        } else {

            AppointmentList(
                appointments = historyAppointments,
                navController = navController
            )
        }
    }
}

@Composable
private fun AppointmentTab(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MediQGreen
        } else {
            Color(0xFFF1F4F2)
        }
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    Color.White
                } else {
                    MediQTextSecondary
                }
            )
        }
    }
}

@Composable
private fun AppointmentList(
    appointments: List<AppointmentData>,
    navController: NavController
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        items(
            items = appointments,
            key = { it.id }
        ) { appointment ->

            AppointmentCard(
                appointment = appointment,
                onClick = {

                    navController.navigate(
                        Screen.AppointmentDetails.createRoute(
                            appointment.id
                        )
                    )
                }
            )
        }
    }
}

data class AppointmentData(
    val id: String,
    val doctor: String,
    val initials: String,
    val specialty: String,
    val date: String,
    val time: String,
    val location: String,
    val reason: String,
    val status: String
)

@Composable
private fun AppointmentCard(
    appointment: AppointmentData,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFDCE3DE)
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            // ─────────────────────────
            // DOCTOR HEADER
            // ─────────────────────────

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = when (appointment.initials) {
                                "KL" -> Color(0xFFFBE7E8)
                                else -> Color(0xFFE7F1EA)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appointment.initials,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (appointment.initials == "KL") {
                            Color(0xFFB4232C)
                        } else {
                            MediQGreen
                        }
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = appointment.doctor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediQTextPrimary
                    )

                    Text(
                        text = appointment.specialty,
                        fontSize = 11.sp,
                        color = MediQTextSecondary
                    )
                }

                StatusChip(
                    status = appointment.status
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(
                color = Color(0xFFE0E5E2)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ─────────────────────────
            // DATE / TIME
            // ─────────────────────────

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MediQTextSecondary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${appointment.date} · ${appointment.time}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MediQTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // ─────────────────────────
            // LOCATION
            // ─────────────────────────

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MediQTextSecondary,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = appointment.location,
                    fontSize = 11.sp,
                    color = MediQTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            // ─────────────────────────
            // REASON
            // ─────────────────────────

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(7.dp),
                color = Color(0xFFF1F5F2)
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
                    )
                ) {

                    Text(
                        text = "Reason:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQTextPrimary
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = appointment.reason,
                        fontSize = 10.sp,
                        color = MediQTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: String
) {

    val backgroundColor: Color
    val textColor: Color

    when (status) {

        "Confirmed" -> {
            backgroundColor = Color(0xFFEAF6ED)
            textColor = MediQGreen
        }

        "Awaiting confirmation" -> {
            backgroundColor = Color(0xFFFFF4E5)
            textColor = Color(0xFFB76E00)
        }

        "Completed" -> {
            backgroundColor = Color(0xFFEAF6ED)
            textColor = MediQGreen
        }

        "Cancelled" -> {
            backgroundColor = Color(0xFFFFE9E7)
            textColor = Color(0xFFB42318)
        }

        else -> {
            backgroundColor = Color(0xFFF1F3F2)
            textColor = MediQTextSecondary
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 5.dp
            ),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}