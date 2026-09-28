package com.example.mediq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
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

// =====================================================
// PROFILE SCREEN
// =====================================================

@Composable
fun ProfileScreen(
    navController: NavController
) {

    var showSignOutDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
    ) {

        // ─────────────────────────────────
        // TOP GREEN HEADER
        // ─────────────────────────────────

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(MediQGreen)
        )

        // ─────────────────────────────────
        // PROFILE CONTENT
        // ─────────────────────────────────

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // Profile identity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MediQLightGreen,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AZ",
                        color = MediQGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column {

                    Text(
                        text = "Arweyne Zoe Salcedo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediQTextPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = "zoe.salcedo@email.com",
                        fontSize = 12.sp,
                        color = MediQTextSecondary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // ─────────────────────────────────
            // PERSONAL DETAILS CARD
            // ─────────────────────────────────

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFDCE3DE)
                )
            ) {

                Column {

                    ProfileDetailRow(
                        label = "Mobile number",
                        value = "0917 442 8810"
                    )

                    HorizontalDivider(
                        color = Color(0xFFE3E7E4)
                    )

                    // Edit button
                    OutlinedButton(
                        onClick = {
                            navController.navigate(
                                Screen.EditProfile.route
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFDCE3DE)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White
                        )
                    ) {

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MediQTextPrimary,
                            modifier = Modifier.size(17.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(7.dp)
                        )

                        Text(
                            text = "Edit my details",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MediQTextPrimary
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // ─────────────────────────────────
            // SIGN OUT
            // ─────────────────────────────────

            OutlinedButton(
                onClick = {
                    showSignOutDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFF2B8B5)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFFFF7F6),
                    contentColor = Color(0xFFB42318)
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Sign out",
                    modifier = Modifier.size(17.dp)
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Text(
                    text = "Sign out",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // ─────────────────────────────────────
    // SIGN OUT DIALOG
    // ─────────────────────────────────────

    if (showSignOutDialog) {

        AlertDialog(
            onDismissRequest = {
                showSignOutDialog = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),

            title = {
                Text(
                    text = "Sign out?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MediQTextPrimary
                )
            },

            text = {
                Text(
                    text = "Are you sure you want to sign out of your MediQ account?",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MediQTextSecondary
                )
            },

            confirmButton = {

                Button(
                    onClick = {

                        showSignOutDialog = false

                        navController.navigate(
                            Screen.SignIn.route
                        ) {
                            popUpTo(0)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFA8161B)
                    )
                ) {
                    Text(
                        text = "Sign out",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },

            dismissButton = {

                OutlinedButton(
                    onClick = {
                        showSignOutDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFDCE3DE)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White
                    )
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MediQTextPrimary
                    )
                }
            }
        )
    }
}

// =====================================================
// PROFILE DETAIL ROW
// =====================================================

@Composable
private fun ProfileDetailRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14.dp,
                vertical = 14.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            fontSize = 12.sp,
            color = MediQTextSecondary
        )

        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MediQTextPrimary
        )
    }
}

// =====================================================
// EDIT MY DETAILS SCREEN
// =====================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController
) {

    var fullName by remember {
        mutableStateOf("Arweyne Zoe Salcedo")
    }

    var phoneNumber by remember {
        mutableStateOf("0917 442 8810")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
    ) {

        // ─────────────────────────────────
        // HEADER
        // ─────────────────────────────────

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
                text = "Edit My Details",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ─────────────────────────────────
        // CONTENT
        // ─────────────────────────────────

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                )
        ) {

            Text(
                text = "Your details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MediQTextPrimary
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Keep your information up to date for your appointments.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MediQTextSecondary
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // ─────────────────────────
            // FULL NAME
            // ─────────────────────────

            Text(
                text = "Full Name",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 13.sp,
                    color = MediQTextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // ─────────────────────────
            // PHONE NUMBER
            // ─────────────────────────

            Text(
                text = "Phone Number",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MediQTextPrimary
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 13.sp,
                    color = MediQTextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MediQGreen,
                    unfocusedBorderColor = Color(0xFFD8DED9),
                    cursorColor = MediQGreen
                )
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // ─────────────────────────
            // SAVE
            // ─────────────────────────

            Button(
                onClick = {
                    // Frontend-only for now.
                    // Later this will update the backend/database.
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediQGreen
                )
            ) {

                Text(
                    text = "Save Changes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}