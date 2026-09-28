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
        startDestination = Screen.SignIn.route
    ) {
        // Auth Flow
        composable(Screen.SignIn.route) { SignInScreen(navController) }
        composable(Screen.RegisterDetails.route) { RegisterDetailsScreen(navController) }
        composable(Screen.RegisterOTP.route) { RegisterOTPScreen(navController) }
        composable(Screen.RegisterCredentials.route) { RegisterCredentialsScreen(navController) }
        composable(Screen.RegisterSuccess.route) { RegisterSuccessScreen(navController) }

        // Main Tabs
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Doctors.route) { DoctorsScreen(navController) }
        composable(Screen.Appointments.route) { AppointmentsScreen(navController) }
        composable(Screen.Messages.route) {
            MessagesScreen(navController)
        }

        composable(Screen.MessageChat.route) { backStackEntry ->

            val conversationId =
                backStackEntry.arguments
                    ?.getString("conversationId")
                    ?: "maria_secretary"

            SecretaryChatScreen(
                navController = navController,
                conversationId = conversationId
            )
        }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController)
        }

        // Details
        composable(Screen.DoctorDetails.route) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            DoctorDetailsScreen(navController, doctorId)
        }
        composable(Screen.BookingFlow.route) { backStackEntry ->

            val doctorId =
                backStackEntry.arguments?.getString("doctorId")

            val dateIndex =
                backStackEntry.arguments
                    ?.getString("dateIndex")
                    ?.toIntOrNull()
                    ?: 0

            val time =
                backStackEntry.arguments
                    ?.getString("time")
                    ?: ""

            BookingFlowScreen(
                navController = navController,
                doctorId = doctorId,
                dateIndex = dateIndex,
                selectedTime = time
            )
        }
        composable(Screen.BookingSuccess.route) { backStackEntry ->

            val dateIndex =
                backStackEntry.arguments
                    ?.getString("dateIndex")
                    ?.toIntOrNull()
                    ?: 0

            val time =
                backStackEntry.arguments
                    ?.getString("time")
                    ?: ""

            BookingSuccessScreen(
                navController = navController,
                dateIndex = dateIndex,
                selectedTime = time
            )
        }
        composable(Screen.AppointmentDetails.route) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId")
            AppointmentDetailsScreen(navController, appointmentId)
        }
        composable(Screen.Notifications.route) { NotificationsScreen(navController) }
    }
}
