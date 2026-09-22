package com.example.mediq.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mediq.ui.screens.*

@Composable
fun MediQNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Auth Flow
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.SignIn.route) { SignInScreen(navController) }
        composable(Screen.RegisterDetails.route) { RegisterDetailsScreen(navController) }
        composable(Screen.RegisterOTP.route) { RegisterOTPScreen(navController) }
        composable(Screen.RegisterCredentials.route) { RegisterCredentialsScreen(navController) }
        composable(Screen.RegisterSuccess.route) { RegisterSuccessScreen(navController) }

        // Main Tabs
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Doctors.route) { DoctorsScreen(navController) }
        composable(Screen.Appointments.route) { AppointmentsScreen(navController) }
        composable(Screen.Messages.route) { PlaceholderScreen("Messages") }
        composable(Screen.Profile.route) { ProfileScreen(navController) }

        // Details
        composable(Screen.DoctorDetails.route) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            DoctorDetailsScreen(navController, doctorId)
        }
        composable(Screen.BookingFlow.route) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            BookingFlowScreen(navController, doctorId)
        }
        composable(Screen.BookingSuccess.route) { BookingSuccessScreen(navController) }
        composable(Screen.AppointmentDetails.route) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId")
            AppointmentDetailsScreen(navController, appointmentId)
        }
        composable(Screen.Notifications.route) { NotificationsScreen(navController) }
    }
}
