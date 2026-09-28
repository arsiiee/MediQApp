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
 * Determines which main tab owns the current screen.
 *
 * This is important because screens such as:
 *
 * DoctorDetails
 * BookingFlow
 * AppointmentDetails
 * BookingSuccess
 * MessageChat
 *
 * are not themselves bottom-navigation tabs.
 */
private fun getMainTab(route: String?): Screen? {

    return when (route) {

        // Home
        Screen.Home.route ->
            Screen.Home

        // Doctor-related screens
        Screen.Doctors.route,
        Screen.DoctorDetails.route,
        Screen.BookingFlow.route ->
            Screen.Doctors

        // Appointment-related screens
        Screen.Appointments.route,
        Screen.AppointmentDetails.route,
        Screen.BookingSuccess.route ->
            Screen.Appointments

        // Messages-related screens
        Screen.Messages.route,
        Screen.MessageChat.route ->
            Screen.Messages

        // Profile
        Screen.Profile.route ->
            Screen.Profile

        else ->
            null
    }
}

@Composable
fun MediQBottomBar(
    navController: NavHostController
) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRoute =
        currentDestination?.route

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
                        imageVector = if (selected) {
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

                    /*
                     * If we're already on the exact root tab,
                     * there is nothing to do.
                     *
                     * Example:
                     * Doctors → tap Doctors = stay there.
                     *
                     * But:
                     * Doctor Profile → tap Doctors
                     * DOES navigate back to Doctors.
                     */

                    val isExactRootScreen =
                        currentRoute == item.screen.route

                    if (!isExactRootScreen) {

                        navController.navigate(
                            item.screen.route
                        ) {

                            /*
                             * Home is the root of the
                             * logged-in part of the app.
                             *
                             * This avoids popping all the
                             * way back to SignIn.
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