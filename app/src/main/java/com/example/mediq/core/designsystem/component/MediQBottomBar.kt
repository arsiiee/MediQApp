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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mediq.core.designsystem.theme.LocalMediQColors
import com.example.mediq.ui.navigation.Screen

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
    /**
     * What a screen reader announces for this destination.
     *
     * Defaults to [label] because for four of the five they are the same word.
     * It is separate because this tab visibly says "Bookings" to fit the bar,
     * while its screen is titled "My appointments". The accessible name starts
     * with the visible label for voice control (WCAG 2.5.3), then names the
     * destination in the same terms as its screen.
     */
    val contentDescription: String = label,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, Icons.Filled.Home, "Home"),
    BottomNavItem(Screen.Doctors, Icons.Filled.MedicalServices, "Doctors"),
    BottomNavItem(Screen.Appointments, Icons.Filled.CalendarMonth, "Bookings", "Bookings, appointments"),
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
                // Material3 merges the icon into its clickable item, dropping the
                // icon's own contentDescription. Name the item itself; the icon
                // is decorative. The on-device semantics test guards this seam.
                modifier = Modifier.semantics { contentDescription = item.contentDescription },
                icon = { Icon(item.icon, contentDescription = null) },
                // Material3 reserves 8 dp between items. "Appointments" wrapped
                // even at 411 dp; "Bookings" fits at 320 dp using the existing
                // labelMedium token. Keep the overflow guard for future labels.
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
