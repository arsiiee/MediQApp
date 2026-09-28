package com.example.mediq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
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
import com.example.mediq.ui.theme.MediQLightGreen
import com.example.mediq.ui.theme.MediQTextPrimary
import com.example.mediq.ui.theme.MediQTextSecondary

@Composable
fun DoctorsScreen(navController: NavController) {

    var searchQuery by remember { mutableStateOf("") }
    var selectedSpecialty by remember { mutableStateOf("All specialties") }
    var selectedLocation by remember { mutableStateOf("All locations") }

    var specialtyExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }

    val specialtyOptions = listOf(
        "All specialties",
        "Internal Medicine",
        "Pediatrics",
        "Cardiology",
        "OB-Gynecology",
        "Orthopedics",
        "Dermatology",
        "Ophthalmology"
    )

    val locationOptions = listOf(
        "All locations",
        "Main Building — 2F",
        "Main Building — 3F",
        "Annex Wing — 1F",
        "Annex Wing — 4F"
    )

    val doctors = listOf(
        DoctorInfo(
            id = "1",
            name = "Dr. Maria Elena Sandoval",
            initials = "ME",
            specialty = "Internal Medicine",
            experience = 14,
            fee = 700,
            location = "Main Building — 2F · Clinic 204",
            openSlots = 13
        ),
        DoctorInfo(
            id = "2",
            name = "Dr. Joel Marquez",
            initials = "JM",
            specialty = "Pediatrics",
            experience = 18,
            fee = 500,
            location = "Annex Wing — 1F · Clinic 108",
            openSlots = 18
        ),
        DoctorInfo(
            id = "3",
            name = "Dr. Antonio Reyes Jr.",
            initials = "AR",
            specialty = "Cardiology",
            experience = 20,
            fee = 1000,
            location = "Main Building — 3F · Heart Station 301",
            openSlots = 10
        ),
        DoctorInfo(
            id = "4",
            name = "Dr. Grace Villanueva",
            initials = "GV",
            specialty = "OB-Gynecology",
            experience = 15,
            fee = 800,
            location = "Annex Wing — 4F · Clinic 402",
            openSlots = 15
        ),
        DoctorInfo(
            id = "5",
            name = "Dr. Ramon Dela Cruz",
            initials = "RD",
            specialty = "Orthopedics",
            experience = 13,
            fee = 900,
            location = "Main Building — 2F · Clinic 212",
            openSlots = 15
        )
    )

    val filteredDoctors = doctors.filter { doctor ->

        val matchesSearch =
            searchQuery.isBlank() ||
                    doctor.name.contains(searchQuery, ignoreCase = true) ||
                    doctor.specialty.contains(searchQuery, ignoreCase = true)

        val matchesSpecialty =
            selectedSpecialty == "All specialties" ||
                    doctor.specialty == selectedSpecialty

        val matchesLocation =
            selectedLocation == "All locations" ||
                    doctor.location.startsWith(selectedLocation)

        matchesSearch &&
                matchesSpecialty &&
                matchesLocation
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF9))
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 18.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Search doctor name or specialty",
                            color = Color(0xFF9AA49F)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF8B9690)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F6F3),
                        unfocusedContainerColor = Color(0xFFF2F6F3),
                        focusedBorderColor = Color(0xFFD7E0DA),
                        unfocusedBorderColor = Color(0xFFD7E0DA),
                        cursorColor = MediQGreen
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    DoctorFilterDropdown(
                        text = selectedSpecialty,
                        options = specialtyOptions,
                        expanded = specialtyExpanded,
                        onExpandedChange = {
                            specialtyExpanded = it
                            if (it) {
                                locationExpanded = false
                            }
                        },
                        onOptionSelected = {
                            selectedSpecialty = it
                            specialtyExpanded = false
                        },
                        modifier = Modifier.weight(1f)
                    )

                    DoctorFilterDropdown(
                        text = selectedLocation,
                        options = locationOptions,
                        expanded = locationExpanded,
                        onExpandedChange = {
                            locationExpanded = it
                            if (it) {
                                specialtyExpanded = false
                            }
                        },
                        onOptionSelected = {
                            selectedLocation = it
                            locationExpanded = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                HorizontalDivider(
                    color = Color(0xFFE4E9E6)
                )
            }

            items(
                items = filteredDoctors,
                key = { it.id }
            ) { doctor ->

                DoctorListItem(
                    doctor = doctor,
                    onClick = {
                        navController.navigate(
                            Screen.DoctorDetails.createRoute(doctor.id)
                        )
                    }
                )
            }

            if (filteredDoctors.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No doctors found",
                            color = MediQTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorFilterDropdown(
    text: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {

        OutlinedButton(
            onClick = {
                onExpandedChange(!expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(
                horizontal = 12.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (expanded) {
                    MediQGreen
                } else {
                    Color(0xFFD8DED9)
                }
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = MediQTextPrimary
            )
        ) {

            Text(
                text = text,
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                maxLines = 1
            )

            Text(
                text = if (expanded) "▲" else "▼",
                fontSize = 10.sp,
                color = MediQTextSecondary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                onExpandedChange(false)
            }
        ) {

            options.forEach { option ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                    }
                )
            }
        }
    }
}

data class DoctorInfo(
    val id: String,
    val name: String,
    val initials: String,
    val specialty: String,
    val experience: Int,
    val fee: Int,
    val location: String,
    val openSlots: Int
)

@Composable
private fun DoctorListItem(
    doctor: DoctorInfo,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 3.dp
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val avatarColor =
                when (doctor.initials) {
                    "ME" -> Color(0xFFE7EEE9)
                    "JM" -> Color(0xFFFBE5E5)
                    "AR" -> Color(0xFFFBE5E5)
                    "GV" -> Color(0xFFE7EEE9)
                    else -> Color(0xFFE7EEE9)
                }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doctor.initials,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MediQTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = doctor.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MediQTextPrimary
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = doctor.specialty,
                    fontSize = 12.sp,
                    color = MediQGreen,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MediQTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = doctor.location,
                        fontSize = 11.sp,
                        color = MediQTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = "${doctor.openSlots} open slots",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MediQGreen
                )
            }
        }
    }
}