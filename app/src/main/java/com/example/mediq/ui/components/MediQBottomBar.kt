package com.example.mediq.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQTextSecondary

data class BottomNavItem(
    val screen: Screen,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Home,
        filledIcon = Icons.Filled.Home,
        outlinedIcon = Icons.Outlined.Home,
        label = "Home"
    ),
    BottomNavItem(
        screen = Screen.Doctors,
        filledIcon = Icons.Filled.MedicalServices,
        outlinedIcon = Icons.Outlined.MedicalServices,
        label = "Doctors"
    ),
    BottomNavItem(
        screen = Screen.Appointments,
        filledIcon = Icons.Filled.CalendarMonth,
        outlinedIcon = Icons.Outlined.CalendarMonth,
        label = "Appointments"
    ),
    BottomNavItem(
        screen = Screen.Messages,
        filledIcon = Icons.Filled.Chat,
        outlinedIcon = Icons.Outlined.Chat,
        label = "Messages"
    ),
    BottomNavItem(
        screen = Screen.Profile,
        filledIcon = Icons.Filled.Person,
        outlinedIcon = Icons.Outlined.Person,
        label = "Profile"
    )
)

/*
 * Determines which main bottom-navigation tab owns the
 * current screen.
 */
private fun getMainTab(route: String?): Screen? {

    return when {

        route == null -> null

        // HOME
        route == Screen.Home.route ||
                route == Screen.Notifications.route -> {
            Screen.Home
        }

        // DOCTORS
        route == Screen.Doctors.route ||
                route == Screen.DoctorDetails.route ||
                route == Screen.BookingFlow.route ||
                route.startsWith("doctor_details/") ||
                route.startsWith("booking_flow/") -> {
            Screen.Doctors
        }

        // APPOINTMENTS
        route == Screen.Appointments.route ||
                route == Screen.AppointmentDetails.route ||
                route == Screen.BookingSuccess.route ||
                route.startsWith("appointment_details/") ||
                route.startsWith("booking_success/") -> {
            Screen.Appointments
        }

        // MESSAGES
        route == Screen.Messages.route ||
                route == Screen.MessageChat.route ||
                route.startsWith("message_chat/") -> {
            Screen.Messages
        }

        // PROFILE
        route == Screen.Profile.route ||
                route == Screen.EditProfile.route -> {
            Screen.Profile
        }

        else -> null
    }
}

@Composable
fun MediQBottomBar(
    navController: NavHostController
) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry?.destination?.route

    val currentMainTab =
        getMainTab(currentRoute)

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {

        bottomNavItems.forEach { item ->

            val selected =
                currentMainTab == item.screen

            NavigationBarItem(

                // ─────────────────────────
                // ICON
                // ─────────────────────────

                icon = {
                    Icon(
                        imageVector =
                            if (selected) {
                                item.filledIcon
                            } else {
                                item.outlinedIcon
                            },
                        contentDescription = item.label
                    )
                },

                // ─────────────────────────
                // LABEL
                // ─────────────────────────

                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp
                    )
                },

                selected = selected,

                // ─────────────────────────
                // NAVIGATION
                // ─────────────────────────

                onClick = {

                    when (item.screen) {

                        // HOME
                        // This explicitly clears all screens above Home.
                        Screen.Home -> {

                            if (currentRoute != Screen.Home.route) {

                                navController.navigate(
                                    Screen.Home.route
                                ) {

                                    popUpTo(
                                        Screen.Home.route
                                    ) {
                                        inclusive = false
                                    }

                                    launchSingleTop = true

                                    // Do not restore a previous deep screen.
                                    restoreState = false
                                }
                            }
                        }

                        // OTHER MAIN TABS
                        else -> {

                            // If we're already on the exact root screen,
                            // don't navigate again.
                            if (currentRoute != item.screen.route) {

                                navController.navigate(
                                    item.screen.route
                                ) {

                                    /*
                                     * Home is the root of the logged-in
                                     * portion of the application.
                                     *
                                     * This removes deep screens such as:
                                     * Doctor Details
                                     * Booking Flow
                                     * Appointment Details
                                     * Message Chat
                                     * Edit Profile
                                     */
                                    popUpTo(
                                        Screen.Home.route
                                    ) {
                                        saveState = true
                                    }

                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                },

                // ─────────────────────────
                // COLORS
                // ─────────────────────────

                colors = NavigationBarItemDefaults.colors(

                    selectedIconColor =
                        MediQGreen,

                    selectedTextColor =
                        MediQGreen,

                    unselectedIconColor =
                        MediQTextSecondary,

                    unselectedTextColor =
                        MediQTextSecondary,

                    indicatorColor =
                        Color.Transparent
                )
            )
        }
    }
}