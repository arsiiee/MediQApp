package com.example.mediq.ui.navigation

sealed class Screen(val route: String) {
    // Auth Flow
    object Splash : Screen("splash")
    object SignIn : Screen("sign_in")
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

    // Detail Screens
    object DoctorDetails : Screen("doctor_details/{doctorId}") {
        fun createRoute(doctorId: String) = "doctor_details/$doctorId"
    }
    object BookingFlow : Screen("booking_flow/{doctorId}") {
        fun createRoute(doctorId: String) = "booking_flow/$doctorId"
    }
    object BookingSuccess : Screen("booking_success")
    object AppointmentDetails : Screen("appointment_details/{appointmentId}") {
        fun createRoute(appointmentId: String) = "appointment_details/$appointmentId"
    }
    object Notifications : Screen("notifications")
}
