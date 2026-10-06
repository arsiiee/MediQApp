package com.example.mediq.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mediq.core.designsystem.theme.MediQGreen
import com.example.mediq.core.designsystem.theme.MediQLightGreen
import com.example.mediq.core.designsystem.theme.MediQSurface
import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.LoadState
import com.example.mediq.ui.navigation.Screen
import java.time.format.DateTimeFormatter

@Composable
fun ProfileScreen(navController: NavController) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        when (val sessionState = uiState.session) {
            is LoadState.Loading -> {
                CircularProgressIndicator(
                    color = MediQGreen,
                    modifier = Modifier.size(36.dp),
                )
            }

            is LoadState.Error -> {
                ProfileOption(
                    icon    = Icons.Outlined.PersonOutline,
                    title   = "Couldn't load profile",
                    onClick = { viewModel.refresh() }
                )
            }

            is LoadState.Success -> {
                val session = sessionState.data
                if (session == null) {
                    // Not signed in
                    ProfileOption(
                        icon    = Icons.Outlined.PersonOutline,
                        title   = "Sign in to view your details",
                        onClick = { navController.navigate(Screen.SignIn.route) }
                    )
                } else {
                    // Signed-in user profile
                    ProfileHeader(session)
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileDetailRow(
                        icon  = Icons.Outlined.Email,
                        label = "Email",
                        value = session.profile.email.ifBlank { "Not provided" },
                    )
                    ProfileDetailRow(
                        icon  = Icons.Outlined.Phone,
                        label = "Mobile",
                        value = session.profile.mobileNumber,
                    )
                    ProfileDetailRow(
                        icon  = Icons.Outlined.Cake,
                        label = "Date of birth",
                        value = session.profile.dateOfBirth
                            .format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                    )
                    if (session.profile.sex != null) {
                        ProfileDetailRow(
                            icon  = Icons.Outlined.AccountCircle,
                            label = "Sex",
                            value = session.profile.sex.displayName,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        ProfileOption(
            icon    = Icons.Outlined.Notifications,
            title   = "Notifications",
            onClick = { navController.navigate(Screen.Notifications.route) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        ProfileOption(
            icon       = Icons.AutoMirrored.Filled.Logout,
            title      = "Sign out",
            isCritical = true,
            onClick    = { showSignOutDialog = true }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title   = { Text("Sign out?") },
            text    = { Text("Are you sure you want to sign out of your MediQ account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut {
                            navController.navigate(Screen.Splash.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(session: AuthSession) {
    val initials = session.profile.fullName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")

    Surface(
        modifier = Modifier.size(72.dp),
        shape    = CircleShape,
        color    = MediQLightGreen,
    ) {
        Text(
            text     = initials,
            modifier = Modifier.padding(top = 20.dp),
            style    = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color    = MediQGreen,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text  = session.profile.fullName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text  = "@${session.profile.username}",
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MediQLightGreen,
    ) {
        Text(
            text     = session.profile.role.wireValue,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = MediQGreen,
        )
    }
}

@Composable
private fun ProfileDetailRow(icon: ImageVector, label: String, value: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MediQSurface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MediQGreen, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
                Text(
                    text  = value,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    isCritical: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = MediQSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isCritical) Color(0xFFD32F2F) else MediQGreen)
            Spacer(modifier = Modifier.padding(horizontal = 16.dp))
            Text(
                text     = title,
                modifier = Modifier.weight(1f),
                color    = if (isCritical) Color(0xFFD32F2F) else Color.Black
            )
            if (trailingText != null) {
                Surface(color = Color(0xFFFFF3E0), shape = CircleShape) {
                    Text(
                        text     = trailingText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color    = Color(0xFFE65100)
                    )
                }
            }
        }
    }
}