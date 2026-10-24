package com.example.mediq.ui.screens.auth

import com.example.mediq.data.model.UserRole

/*
 * What each auth screen can be doing, and what it asks its caller to do.
 *
 * The states live here so each screen's layout can live in its own file and
 * read as one shape of one screen.
 */

/** Where a sign-in attempt has got to. */
sealed interface SignInState {

    /** Nothing submitted yet. */
    data object Idle : SignInState

    /** The credentials are being checked. */
    data object SigningIn : SignInState

    /**
     * The pair was rejected. The message says the pair does not match rather
     * than naming which half was wrong, which is also what a real sign-in
     * would do.
     */
    data class InvalidCredentials(val message: String) : SignInState

    /** Anything else that stopped the sign-in; says what happened. */
    data class Error(val message: String) : SignInState
}

/** Where the registration steps have got to. Shared by all four. */
sealed interface RegisterState {

    /** The step is open and nothing has been sent. */
    data object Idle : RegisterState

    /** This step is on its way to the server. */
    data object Submitting : RegisterState

    /** The step was rejected; the message says why. */
    data class Error(val message: String) : RegisterState
}

/** The extra life cycle the one-time-code step has on top of [RegisterState]. */
sealed interface OtpState {

    /** The code is with the patient and is still valid. */
    data object Sent : OtpState

    /** The code ran out before it was used, so only a new one helps. */
    data object Expired : OtpState

    /** The code is being checked. */
    data object Verifying : OtpState

    /** The code was rejected, or the check could not run. */
    data class Error(val message: String) : OtpState
}

/** What the sign-in screen asks for. */
data class SignInActions(
    val onSubmit: (username: String, password: String) -> Unit = { _, _ -> },
    val onForgotPassword: () -> Unit = {},
    val onRegister: () -> Unit = {}
)

/** What the four registration steps ask for. */
data class RegisterActions(
    val onBack: () -> Unit = {},
    val onDetailsContinue: (fullName: String, phoneNumber: String, role: UserRole) -> Unit = { _, _, _ -> },
    val onOtpVerify: (code: String) -> Unit = {},
    val onOtpResend: () -> Unit = {},
    val onCredentialsCreate: (username: String, password: String) -> Unit = { _, _ -> },
    val onGoToSignIn: () -> Unit = {}
)
