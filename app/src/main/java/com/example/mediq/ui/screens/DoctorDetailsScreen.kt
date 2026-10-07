package com.example.mediq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.mediq.ui.theme.MediQSurface
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

private val ALL_TIME_SLOTS = listOf(
    "9:00 AM",
    "9:30 AM",
    "10:00 AM",
    "10:30 AM",
    "11:00 AM",
    "11:30 AM",
    "12:00 PM",
    "12:30 PM",
    "1:00 PM",
    "1:30 PM",
    "2:00 PM",
    "2:30 PM",
    "3:00 PM",
    "3:30 PM",
    "4:00 PM",
    "4:30 PM"
)

@Composable
fun DoctorDetailsScreen(
    navController: NavController,
    doctorId: String?
) {
    var selectedDate by remember { mutableStateOf(0) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    val dates = listOf(
        DateOption("Mon", 21, 1),
        DateOption("Wed", 23, 4),
        DateOption("Fri", 25, 2),
        DateOption("Mon", 28, 2),
        DateOption("Wed", 30, 3)
    )

    val selectedDateInfo = dates[selectedDate]
    val slotStatuses = slotStatusesFor(selectedDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
    ) {

        // ─────────────────────────────
        // Header
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
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Doctors Profile",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ─────────────────────────────
        // Scrollable content
        // ─────────────────────────────

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // Doctor information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(
                        horizontal = 20.dp,
                        vertical = 18.dp
                    )
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE7EEE9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ME",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MediQTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Dr. Maria Elena Sandoval",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MediQTextPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Internal Medicine",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MediQGreen
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MediQTextSecondary,
                                modifier = Modifier.size(17.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "Main Building — 2F · Clinic 204",
                                fontSize = 12.sp,
                                color = MediQTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Experience + consultation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    DoctorInfoCard(
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MediQTextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        label = "Experience",
                        value = "14 years"
                    )

                    DoctorInfoCard(
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MediQTextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        label = "Consultation",
                        value = "₱700"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Adult internal medicine with focus on diabetes, hypertension, and preventive check-ups. Consults in Filipino, Cebuano, and English.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MediQTextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Clinic hours card
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

                        Text(
                            text = "Clinic hours",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8A9690)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        ClinicHourRow(
                            day = "Monday",
                            time = "9:00 AM – 5:00 PM"
                        )

                        ClinicHourRow(
                            day = "Wednesday",
                            time = "9:00 AM – 5:00 PM"
                        )

                        ClinicHourRow(
                            day = "Friday",
                            time = "9:00 AM – 5:00 PM"
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "License no. PRC 0112843",
                            fontSize = 12.sp,
                            color = Color(0xFF98A29D)
                        )
                    }
                }
            }

            // ─────────────────────────────
            // Time-slot section
            // ─────────────────────────────

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 18.dp
                    )
            ) {

                Text(
                    text = "Choose a time slot",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MediQTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Clinic hours are 9:00 AM–5:00 PM. Reserved and blocked slots cannot be selected.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MediQTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Date selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    dates.forEachIndexed { index, date ->

                        DateCard(
                            date = date,
                            selected = selectedDate == index,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedDate = index
                                selectedTime = null
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time slots
                ALL_TIME_SLOTS
                    .chunked(3)
                    .forEach { rowSlots ->

                        TimeSlotRow(
                            slots = rowSlots,
                            statuses = slotStatuses,
                            selectedTime = selectedTime,
                            onSelect = {
                                selectedTime = it
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    AvailabilityLegend(
                        color = Color(0xFF329653),
                        label = "Available"
                    )

                    AvailabilityLegend(
                        color = Color(0xFF919C97),
                        label = "Reserved"
                    )

                    AvailabilityLegend(
                        color = Color(0xFFE14A59),
                        label = "Blocked"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Continue
                Button(
                    onClick = {
                        selectedTime?.let { time ->

                            navController.navigate(
                                Screen.BookingFlow.createRoute(
                                    doctorId = doctorId ?: "1",
                                    dateIndex = selectedDateInfo.date,
                                    time = time
                                )
                            )
                        }
                    },
                    enabled = selectedTime != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediQGreen,
                        disabledContainerColor = Color(0xFF8CBA99),
                        disabledContentColor = Color.White
                    )
                ) {

                    Text(
                        text = selectedTime?.let {
                            "Continue · $it"
                        } ?: "Select a slot to continue",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun DoctorInfoCard(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MediQSurface
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                icon()

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MediQTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MediQTextPrimary
            )
        }
    }
}

@Composable
private fun ClinicHourRow(
    day: String,
    time: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = day,
            fontSize = 13.sp,
            color = MediQTextPrimary
        )

        Text(
            text = time,
            fontSize = 13.sp,
            color = MediQTextSecondary
        )
    }
}

data class DateOption(
    val day: String,
    val date: Int,
    val openSlots: Int
)

@Composable
private fun DateCard(
    date: DateOption,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {

    Surface(
        modifier = modifier
            .height(78.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(13.dp),
        color = if (selected) {
            MediQGreen
        } else {
            Color.White
        },
        border = if (selected) {
            null
        } else {
            BorderStroke(
                width = 1.dp,
                color = Color(0xFFDCE3DE)
            )
        }
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = date.day,
                fontSize = 12.sp,
                color = if (selected) {
                    Color.White
                } else {
                    MediQTextSecondary
                }
            )

            Text(
                text = date.date.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    Color.White
                } else {
                    MediQTextPrimary
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${date.openSlots} open",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    Color.White
                } else {
                    MediQGreen
                }
            )
        }
    }
}

enum class TimeSlotStatus {
    AVAILABLE,
    RESERVED,
    BLOCKED
}

private fun slotStatusesFor(
    dateIndex: Int
): Map<String, TimeSlotStatus> {

    return when (dateIndex) {

        // Monday 21 — 1 open
        0 -> buildSlotStatuses(
            available = setOf(
                "10:00 AM"
            ),
            blocked = setOf(
                "1:00 PM",
                "3:30 PM",
                "4:30 PM"
            )
        )

        // Wednesday 23 — 4 open
        1 -> buildSlotStatuses(
            available = setOf(
                "9:00 AM",
                "9:30 AM",
                "10:00 AM",
                "10:30 AM"
            ),
            blocked = setOf(
                "11:30 AM",
                "2:00 PM",
                "4:30 PM"
            )
        )

        // Friday 25 — 2 open
        2 -> buildSlotStatuses(
            available = setOf(
                "10:00 AM",
                "10:30 AM"
            ),
            blocked = setOf(
                "11:30 AM",
                "1:30 PM",
                "3:30 PM",
                "4:30 PM"
            )
        )

        // Monday 28 — 2 open
        3 -> buildSlotStatuses(
            available = setOf(
                "9:30 AM",
                "11:00 AM"
            ),
            blocked = setOf(
                "11:30 AM",
                "2:30 PM",
                "4:00 PM"
            )
        )

        // Wednesday 30 — 3 open
        else -> buildSlotStatuses(
            available = setOf(
                "9:00 AM",
                "10:00 AM",
                "11:00 AM"
            ),
            blocked = setOf(
                "11:30 AM",
                "3:00 PM",
                "4:30 PM"
            )
        )
    }
}

private fun buildSlotStatuses(
    available: Set<String>,
    blocked: Set<String>
): Map<String, TimeSlotStatus> {

    return ALL_TIME_SLOTS.associateWith { time ->

        when {
            time in available -> TimeSlotStatus.AVAILABLE
            time in blocked -> TimeSlotStatus.BLOCKED
            else -> TimeSlotStatus.RESERVED
        }
    }
}

@Composable
private fun TimeSlotRow(
    slots: List<String>,
    statuses: Map<String, TimeSlotStatus>,
    selectedTime: String?,
    onSelect: (String) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        slots.forEach { time ->

            val status = statuses[time] ?: TimeSlotStatus.RESERVED

            TimeSlotButton(
                time = time,
                status = status,
                selected = selectedTime == time,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (status == TimeSlotStatus.AVAILABLE) {
                        onSelect(time)
                    }
                }
            )
        }

        // Keeps the final row aligned when it has fewer than 3 slots.
        repeat(3 - slots.size) {
            Spacer(
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimeSlotButton(
    time: String,
    status: TimeSlotStatus,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {

    val backgroundColor = when {
        selected -> MediQGreen
        status == TimeSlotStatus.AVAILABLE -> Color(0xFFF5FAF6)
        status == TimeSlotStatus.BLOCKED -> Color(0xFFF0F2F1)
        else -> Color(0xFFE8EEEA)
    }

    val textColor = when {
        selected -> Color.White
        status == TimeSlotStatus.AVAILABLE -> MediQGreen
        else -> Color(0xFF96A19B)
    }

    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable(
                enabled = status == TimeSlotStatus.AVAILABLE,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (
            status == TimeSlotStatus.AVAILABLE &&
            !selected
        ) {
            BorderStroke(
                width = 1.dp,
                color = Color(0xFFB5D9BF)
            )
        } else {
            null
        }
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = time,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
private fun AvailabilityLegend(
    color: Color,
    label: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            color = MediQTextSecondary
        )
    }
}