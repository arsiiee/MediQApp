package com.example.mediq.ui.screens.auth

/*
 * The named auth states. One place, so a preview and a screen default cannot
 * drift apart, and so the copy the patient reads is written once.
 */
object AuthSamples {

    const val FULL_NAME = "Kissie Ann Apple"
    const val PHONE_NUMBER = "+63 917 555 0142"
    const val USERNAME = "kissie.apple"

    // Sign-in

    val signInIdle: SignInState = SignInState.Idle

    val signingIn: SignInState = SignInState.SigningIn

    val invalidCredentials: SignInState = SignInState.InvalidCredentials(
        "That username and password do not match. Check both and try again."
    )

    val signInError: SignInState = SignInState.Error(
        "We could not reach MediQ. Check your connection and try again."
    )

    // Registration

    val registerIdle: RegisterState = RegisterState.Idle

    val registering: RegisterState = RegisterState.Submitting

    val detailsError: RegisterState = RegisterState.Error(
        "We could not send the code. Check your connection and try again."
    )

    val usernameTaken: RegisterState = RegisterState.Error(
        "That username is already taken. Choose another one."
    )

    // One-time code

    val otpSent: OtpState = OtpState.Sent
    val otpExpired: OtpState = OtpState.Expired
    val otpVerifying: OtpState = OtpState.Verifying

    val otpRejected: OtpState = OtpState.Error(
        "That code is not right. Check the six digits and try again."
    )

    /** A code step where Verify was pressed before all six digits were typed. */
    val otpIncomplete: String? = AuthValidation.validateOtp("123")

    /** What the form steps show when they are submitted empty. */
    val signInErrors: SignInErrors = AuthValidation.validateSignIn("", "")

    val detailsErrors: DetailsErrors = AuthValidation.validateDetails("", "0917")

    val credentialsErrors: CredentialsErrors =
        AuthValidation.validateCredentials("ki", "short", "other")
}
