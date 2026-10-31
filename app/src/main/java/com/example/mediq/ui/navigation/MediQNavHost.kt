package com.example.mediq.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mediq.ui.screens.*
import com.example.mediq.ui.screens.appointmentdetails.AppointmentDetailsScreen
import com.example.mediq.ui.screens.appointments.AppointmentsScreen
import com.example.mediq.ui.screens.auth.BiometricUnlockScreen
import com.example.mediq.ui.screens.auth.ForgotPasswordScreen
import com.example.mediq.ui.screens.auth.SplashScreen
import com.example.mediq.ui.screens.auth.RegisterCredentialsScreen
import com.example.mediq.ui.screens.auth.RegisterDetailsScreen
import com.example.mediq.ui.screens.auth.RegisterOtpScreen
import com.example.mediq.ui.screens.auth.RegisterSuccessScreen
import com.example.mediq.ui.screens.auth.ResetPasswordScreen
import com.example.mediq.ui.screens.auth.SignInScreen
import com.example.mediq.ui.screens.booking.BookingFlowScreen
import com.example.mediq.ui.screens.booking.BookingSuccessScreen
import com.example.mediq.ui.screens.doctordetails.DoctorDetailsScreen
import com.example.mediq.ui.screens.doctors.DoctorsScreen
import com.example.mediq.ui.screens.home.HomeScreen
import com.example.mediq.ui.screens.messages.MessagesScreen
import com.example.mediq.ui.screens.messages.MockConversations
import com.example.mediq.ui.screens.notifications.NotificationsScreen
import com.example.mediq.ui.screens.profile.EditProfileScreen
import com.example.mediq.ui.screens.profile.ProfileScreen
import com.example.mediq.ui.screens.secretarychat.ChatUiState
import com.example.mediq.ui.screens.secretarychat.SecretaryChatScreen
import com.example.mediq.ui.screens.secretarychat.SecretaryChatSamples

@Composable
fun MediQNavHost(navController: NavHostController, startDestination: String = Screen.SignIn.route) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Flow
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.BiometricUnlock.route) { BiometricUnlockScreen(navController) }
        composable(Screen.SignIn.route) { SignInScreen(navController) }
        composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }
        composable(Screen.ResetPassword.route) { ResetPasswordScreen(navController) }
        composable(Screen.RegisterDetails.route) { RegisterDetailsScreen(navController) }
        composable(Screen.RegisterOTP.route) { RegisterOtpScreen(navController) }
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

            // Resolve the secretary's identity from the sample conversation so
            // the header shows name, role, and location instead of defaults.
            val content = (when (conversationId) {
                "maria_secretary" -> SecretaryChatSamples.maria
                "kathleen_secretary" -> SecretaryChatSamples.kathleen
                else -> null
            }) as? ChatUiState.Content

            SecretaryChatScreen(
                navController = navController,
                conversationId = conversationId,
                secretaryName = content?.secretaryName ?: "Secretary",
                role = content?.role,
                location = content?.location,
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
