package com.example.mediq.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.ui.navigation.Screen

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Filled.Home, "Home"),
    BottomNavItem(Screen.Doctors, Icons.Filled.MedicalServices, "Doctors"),
    BottomNavItem(Screen.Appointments, Icons.Filled.CalendarMonth, "Appointments"),
    BottomNavItem(Screen.Messages, Icons.AutoMirrored.Filled.Chat, "Messages"),
    BottomNavItem(Screen.Profile, Icons.Filled.Person, "Profile")
)

@Composable
fun MediQBottomBar(navController: NavHostController) {
    val colors = LocalMediQColors.current
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = colors.accent
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accent,
                    unselectedIconColor = colors.secondaryText,
                    selectedTextColor = colors.accent,
                    unselectedTextColor = colors.secondaryText,
                    indicatorColor = colors.accent.copy(alpha = 0.1f)
                )
            )
        }
    }
}
