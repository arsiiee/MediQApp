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
import com.example.mediq.ui.screens.notifications.NotificationsScreen
import com.example.mediq.ui.screens.profile.EditProfileScreen
import com.example.mediq.ui.screens.profile.ProfileScreen
import com.example.mediq.ui.screens.secretarychat.SecretaryChatScreen

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

            val secretaryName =
                backStackEntry.arguments
                    ?.getString("secretaryName")
                    ?: "Clinic"

            val doctorName =
                backStackEntry.arguments
                    ?.getString("doctorName")
                    ?: "Doctor"

            val appointmentDate =
                backStackEntry.arguments
                    ?.getString("appointmentDate")
                    ?: ""

            val appointmentTime =
                backStackEntry.arguments
                    ?.getString("appointmentTime")
                    ?: ""

            SecretaryChatScreen(
                navController = navController,
                conversationId = conversationId,
                secretaryName = secretaryName,
                doctorName = doctorName,
                appointmentDate = appointmentDate,
                appointmentTime = appointmentTime,
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

            val date =
                backStackEntry.arguments?.getString("date")
                    ?: ""

            val time =
                backStackEntry.arguments
                    ?.getString("time")
                    ?: ""

            BookingFlowScreen(
                navController = navController,
                doctorId = doctorId,
                date = date,
                selectedTime = time
            )
        }
        composable(Screen.BookingSuccess.route) { backStackEntry ->

            val date =
                backStackEntry.arguments?.getString("date") ?: ""

            val time =
                backStackEntry.arguments
                    ?.getString("time")
                    ?: ""

            val doctorName =
                backStackEntry.arguments
                    ?.getString("doctorName")
                    ?: ""

            val specialty =
                backStackEntry.arguments
                    ?.getString("specialty")
                    ?: ""

            val location =
                backStackEntry.arguments
                    ?.getString("location")
                    ?: ""

            val dateLabel =
                backStackEntry.arguments
                    ?.getString("dateLabel")
                    ?: ""

            BookingSuccessScreen(
                navController = navController,
                date = date,
                selectedTime = time,
                doctorName = doctorName,
                specialty = specialty,
                location = location,
                dateLabel = dateLabel
            )
        }
        composable(Screen.AppointmentDetails.route) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId")
            AppointmentDetailsScreen(navController, appointmentId)
        }
        composable(Screen.Notifications.route) { NotificationsScreen(navController) }
    }
}
