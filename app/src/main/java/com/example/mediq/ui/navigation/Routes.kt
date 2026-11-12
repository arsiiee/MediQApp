package com.example.mediq.ui.navigation

sealed class Screen(val route: String) {

    // Auth Flow
    object Splash : Screen("splash")
    object BiometricUnlock : Screen("biometric_unlock")
    object SignIn : Screen("sign_in")
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password")
    object RegisterDetails : Screen("register_details")
    object RegisterOTP : Screen("register_otp")
    object RegisterCredentials : Screen("register_credentials")
    object RegisterSuccess : Screen("register_success")

    // Main Tabs
    object Home : Screen("home")
    object Doctors : Screen("doctors")
    object Appointments : Screen("appointments")
    object Messages : Screen("messages")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")

    // Message Chat
    object MessageChat : Screen("message_chat/{conversationId}/{secretaryName}/{doctorName}/{appointmentDate}/{appointmentTime}") {
        fun createRoute(
            conversationId: String,
            secretaryName: String,
            doctorName: String,
            appointmentDate: String,
            appointmentTime: String,
        ) =
            "message_chat/$conversationId/${android.net.Uri.encode(secretaryName)}/${android.net.Uri.encode(doctorName)}/${android.net.Uri.encode(appointmentDate)}/${android.net.Uri.encode(appointmentTime)}"
    }

    // Detail Screens
    object DoctorDetails : Screen("doctor_details/{doctorId}") {
        fun createRoute(doctorId: String) =
            "doctor_details/$doctorId"
    }

    object BookingFlow : Screen("booking_flow/{doctorId}/{date}/{time}") {
        fun createRoute(
            doctorId: String,
            date: String,
            time: String
        ) = "booking_flow/$doctorId/${android.net.Uri.encode(date)}/${android.net.Uri.encode(time)}"
    }

    object BookingSuccess : Screen("booking_success/{date}/{time}/{doctorName}/{specialty}/{location}/{dateLabel}") {
        fun createRoute(
            date: String,
            time: String,
            doctorName: String,
            specialty: String,
            location: String,
            dateLabel: String
        ) = "booking_success/${android.net.Uri.encode(date)}/${android.net.Uri.encode(time)}/${android.net.Uri.encode(doctorName)}/${android.net.Uri.encode(specialty)}/${android.net.Uri.encode(location)}/${android.net.Uri.encode(dateLabel)}"
    }

    object AppointmentDetails : Screen("appointment_details/{appointmentId}") {
        fun createRoute(appointmentId: String) =
            "appointment_details/$appointmentId"
    }

    object Notifications : Screen("notifications")
}