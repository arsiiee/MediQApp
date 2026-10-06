package com.example.mediq.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mediq.ui.feature.appointments.AppointmentDetailsScreen
import com.example.mediq.ui.feature.appointments.AppointmentsScreen
import com.example.mediq.ui.feature.auth.register.RegisterCredentialsScreen
import com.example.mediq.ui.feature.auth.register.RegisterDetailsScreen
import com.example.mediq.ui.feature.auth.register.RegisterOTPScreen
import com.example.mediq.ui.feature.auth.register.RegisterSuccessScreen
import com.example.mediq.ui.feature.auth.signin.SignInScreen
import com.example.mediq.ui.feature.auth.splash.SplashScreen
import com.example.mediq.ui.feature.booking.BookingFlowScreen
import com.example.mediq.ui.feature.booking.BookingSuccessScreen
import com.example.mediq.ui.feature.doctors.DoctorDetailsScreen
import com.example.mediq.ui.feature.doctors.DoctorsScreen
import com.example.mediq.ui.feature.home.HomeScreen
import com.example.mediq.ui.feature.messages.MessagesScreen
import com.example.mediq.ui.feature.notifications.NotificationsScreen
import com.example.mediq.ui.feature.profile.ProfileScreen

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
        composable(Screen.Messages.route) { MessagesScreen(navController) }
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
