package com.example.mediq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mediq.ui.navigation.Screen
import com.example.mediq.ui.theme.MediQBackground
import com.example.mediq.ui.theme.MediQElevation
import com.example.mediq.ui.theme.MediQBorder
import com.example.mediq.ui.theme.MediQGreen
import com.example.mediq.ui.theme.MediQSize
import com.example.mediq.ui.theme.MediQTextSecondary

/** One tab in the bottom bar: its destination, its two icon states, its label. */
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
        filledIcon = Icons.AutoMirrored.Filled.Chat,
        outlinedIcon = Icons.AutoMirrored.Outlined.Chat,
        label = "Messages"
    ),
    BottomNavItem(
        screen = Screen.Profile,
        filledIcon = Icons.Filled.Person,
        outlinedIcon = Icons.Outlined.Person,
        label = "Profile"
    )
)

/**
 * The main tab that owns [route], or null when the route sits outside the tab bar.
 *
 * Single source of truth: both the bar's selection state and whether the bar is
 * shown at all are derived from this. Deep screens resolve to their owning tab,
 * so the patient keeps their place in the navigation.
 */
internal fun mainTabForRoute(route: String?): Screen? = when {

    route == null -> null

    route == Screen.Home.route ||
            route == Screen.Notifications.route -> Screen.Home

    route == Screen.Doctors.route ||
            route == Screen.DoctorDetails.route ||
            route.startsWith("doctor_details/") ||
            route == Screen.BookingFlow.route ||
            route.startsWith("booking_flow/") -> Screen.Doctors

    route == Screen.Appointments.route ||
            route == Screen.AppointmentDetails.route ||
            route.startsWith("appointment_details/") ||
            route == Screen.BookingSuccess.route ||
            route.startsWith("booking_success/") -> Screen.Appointments

    route == Screen.Messages.route ||
            route == Screen.MessageChat.route ||
            route.startsWith("message_chat/") -> Screen.Messages

    route == Screen.Profile.route ||
            route == Screen.EditProfile.route -> Screen.Profile

    else -> null
}

@Composable
fun MediQBottomBar(navController: NavHostController) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentMainTab = mainTabForRoute(currentRoute)

    Column(modifier = Modifier.background(MediQBackground)) {

        // Hairline separator, matching every card outline in the design.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MediQSize.hairline)
                .background(MediQBorder)
        )

        NavigationBar(
            containerColor = MediQBackground,
            tonalElevation = MediQElevation.none
        ) {

            bottomNavItems.forEach { item ->

                val selected = currentMainTab == item.screen

                NavigationBarItem(
                    icon = {
                        Icon(
                            // Filled when selected, outlined otherwise: shape carries
                            // the state as well as colour.
                            imageVector = if (selected) item.filledIcon else item.outlinedIcon,
                            // The label below already names the tab, so the icon
                            // must not repeat it to a screen reader.
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    selected = selected,
                    onClick = { onTabClick(navController, item.screen, currentRoute) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MediQGreen,
                        selectedTextColor = MediQGreen,
                        unselectedIconColor = MediQTextSecondary,
                        unselectedTextColor = MediQTextSecondary,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

/**
 * Returns the patient to a tab, clearing any deep screens stacked on top of it.
 *
 * Home resets rather than restores: a previous deep screen would otherwise be
 * restored on top of the home tab.
 */
private fun onTabClick(
    navController: NavHostController,
    destination: Screen,
    currentRoute: String?
) {
    if (currentRoute == destination.route) return

    navController.navigate(destination.route) {
        popUpTo(Screen.Home.route) {
            saveState = destination != Screen.Home
        }
        launchSingleTop = true
        restoreState = destination != Screen.Home
    }
}
