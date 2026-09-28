package com.example.mediq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

@Composable
fun NotificationsScreen(
    navController: NavController
) {

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
                .padding(horizontal = 8.dp),
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
                text = "Notifications",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ─────────────────────────────
        // NOTIFICATIONS
        // ─────────────────────────────

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            NotificationCard(
                iconType = NotificationIcon.CONFIRMED,
                title = "Appointment confirmed",
                message = "Your consultation with Dr. Maria Elena Sandoval " +
                        "has been confirmed for Monday, Sep 14.",
                time = "2 hrs ago",
                unread = true
            )

            NotificationCard(
                iconType = NotificationIcon.REMINDER,
                title = "Reminder: Tomorrow",
                message = "Please arrive 15 minutes early for your appointment " +
                        "with Dr. Sandoval tomorrow at 9:30 AM.",
                time = "1 day ago",
                unread = false
            )

            NotificationCard(
                iconType = NotificationIcon.RESCHEDULED,
                title = "Reschedule approved",
                message = "Your request to reschedule Dr. Kathleen Lim's slot " +
                        "has been approved for Thursday, Sep 17.",
                time = "2 days ago",
                unread = false
            )

            NotificationCard(
                iconType = NotificationIcon.CANCELLED,
                title = "Appointment cancelled",
                message = "Your booking request for Dr. Ramon Dela Cruz on " +
                        "Sep 11 has been cancelled.",
                time = "3 days ago",
                unread = false
            )
        }
    }
}

// ─────────────────────────────────────────────
// NOTIFICATION TYPES
// ─────────────────────────────────────────────

private enum class NotificationIcon {
    CONFIRMED,
    REMINDER,
    RESCHEDULED,
    CANCELLED
}

// ─────────────────────────────────────────────
// NOTIFICATION CARD
// ─────────────────────────────────────────────

@Composable
private fun NotificationCard(
    iconType: NotificationIcon,
    title: String,
    message: String,
    time: String,
    unread: Boolean
) {

    val iconBackground: Color
    val iconColor: Color

    when (iconType) {

        NotificationIcon.CONFIRMED -> {
            iconBackground = Color(0xFFE8F4EB)
            iconColor = MediQGreen
        }

        NotificationIcon.REMINDER -> {
            iconBackground = Color(0xFFFFF6E6)
            iconColor = Color(0xFFF59E0B)
        }

        NotificationIcon.RESCHEDULED -> {
            iconBackground = Color(0xFFE8F4EB)
            iconColor = MediQGreen
        }

        NotificationIcon.CANCELLED -> {
            iconBackground = Color(0xFFFBE9E8)
            iconColor = Color(0xFFB42318)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {

            // ─────────────────────────
            // ICON
            // ─────────────────────────

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = iconBackground,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                when (iconType) {

                    NotificationIcon.CONFIRMED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    NotificationIcon.REMINDER -> {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    NotificationIcon.RESCHEDULED -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    NotificationIcon.CANCELLED -> {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            // ─────────────────────────
            // TEXT
            // ─────────────────────────

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {

                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediQTextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (unread) {

                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(6.dp)
                                .background(
                                    color = MediQGreen,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = message,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MediQTextSecondary
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = time,
                    fontSize = 10.sp,
                    color = Color(0xFFA4ADA8)
                )
            }
        }
    }
}