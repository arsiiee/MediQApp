package com.example.mediq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.zIndex

@Composable
fun HomeScreen(navController: NavController) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // Green header behind the consultation card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .background(MediQGreen)
        )

        // Header content
        HomeHeader(
            navController = navController,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(2f)
        )

        // Scrollable home content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 90.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                NextConsultationCard(navController)
            }

            item {
                BookConsultationButton(navController)
            }

            item {
                BrowseBySpecialty()
            }

            item {
                MostOpenSlots(navController)
            }
        }
    }
}

@Composable
fun HomeHeader(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(
            horizontal = 20.dp,
            vertical = 18.dp
        ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Initials avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D8650)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AZ",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Good day,",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )

                Text(
                    text = "Arweyne",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Notification button
        Box(
            modifier = Modifier.size(42.dp)
        ) {

            IconButton(
                onClick = {
                    navController.navigate(Screen.Notifications.route)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF2D8650))
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White
                )
            }

            // Notification indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .align(Alignment.TopEnd)
                    .offset(x = (-3).dp, y = 3.dp)
            )
        }
    }
}

@Composable
fun NextConsultationCard(navController: NavController) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            // Card title + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Your next consultation",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MediQTextSecondary
                )

                Surface(
                    color = MediQLightGreen,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Confirmed",
                        modifier = Modifier.padding(
                            horizontal = 11.dp,
                            vertical = 6.dp
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "9:30 AM",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = MediQTextPrimary
            )

            Text(
                text = "Monday, September 14, 2026",
                fontSize = 13.sp,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = Color(0xFFE7EAE8)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Dr. Maria Elena Sandoval",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Internal Medicine",
                fontSize = 13.sp,
                color = MediQTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MediQTextSecondary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Main Building — 2F — Clinic 204",
                    fontSize = 12.sp,
                    color = MediQTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MediQTextSecondary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Arrive 15 minutes early for registration",
                    fontSize = 12.sp,
                    color = MediQTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            TextButton(
                onClick = {
                    navController.navigate(
                        Screen.AppointmentDetails.createRoute("1")
                    )
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Manage appointment",
                    color = MediQGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(5.dp))

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MediQGreen,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
fun BookConsultationButton(navController: NavController) {

    Button(
        onClick = {
            navController.navigate(Screen.Doctors.route)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MediQGreen
        )
    ) {

        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Book a consultation",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BrowseBySpecialty() {

    val specialties = listOf(
        "Internal Medicine",
        "Pediatrics",
        "Cardiology",
        "Dermatology",
        "OB-Gynecology",
        "Orthopedics",
        "Ophthalmology"
    )

    Column {

        Text(
            text = "Browse by specialty",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MediQTextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(specialties) { specialty ->

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFDCE2DE)
                    ),
                    modifier = Modifier.clickable { }
                ) {

                    Text(
                        text = specialty,
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 9.dp
                        ),
                        fontSize = 12.sp,
                        color = MediQTextPrimary
                    )
                }
            }
        }
    }
}

data class DoctorSlot(
    val name: String,
    val specialtyAndLocation: String,
    val slots: Int
)

@Composable
fun MostOpenSlots(navController: NavController) {

    val doctors = listOf(
        DoctorSlot(
            "Dr. Joel Marquez",
            "Pediatrics · Annex Wing — 1F",
            18
        ),
        DoctorSlot(
            "Dr. Grace Villanueva",
            "OB-Gynecology · Annex Wing — 4F",
            15
        ),
        DoctorSlot(
            "Dr. Ramon Dela Cruz",
            "Orthopedics · Main Building — 2F",
            15
        )
    )

    Column {

        Text(
            text = "Most open slots this week",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MediQTextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFFDCE2DE)
            )
        ) {

            doctors.forEachIndexed { index, doctor ->

                DoctorSlotItem(
                    doctor = doctor,
                    onClick = {
                        navController.navigate(
                            Screen.DoctorDetails.createRoute("1")
                        )
                    }
                )

                if (index < doctors.lastIndex) {
                    HorizontalDivider(
                        color = Color(0xFFE5E8E5)
                    )
                }
            }
        }
    }
}

@Composable
fun DoctorSlotItem(
    doctor: DoctorSlot,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = 16.dp,
                vertical = 13.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = doctor.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = doctor.specialtyAndLocation,
                fontSize = 11.sp,
                color = MediQTextSecondary
            )
        }

        Text(
            text = doctor.slots.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MediQGreen
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "View doctor",
            tint = Color(0xFF87918C),
            modifier = Modifier.size(19.dp)
        )
    }
}