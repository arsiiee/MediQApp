package com.example.mediq.ui.screens.auth

/*
 * The form rules for the auth flow.
 *
 * Plain Kotlin, no Compose and no Android, so the rules can be pinned down by
 * AuthValidationTest rather than by typing into a device. The screens only
 * render what these functions return.
 */

/** Errors under each field of the details step; `null` where the input is fine. */
data class DetailsErrors(
    val fullName: String?,
    val email: String?,
    val phoneNumber: String?,
    val dateOfBirth: String?,
) {
    /** True when nothing on this step needs the patient's attention. */
    val isClean: Boolean
        get() = fullName == null && email == null && phoneNumber == null && dateOfBirth == null
}

/** Errors under each field of the credentials step. */
data class CredentialsErrors(
    val username: String?,
    val password: String?,
    val confirmation: String?
) {
    val isClean: Boolean get() = username == null && password == null && confirmation == null
}

/** Errors under the two sign-in fields. */
data class SignInErrors(
    val username: String?,
    val password: String?
) {
    val isClean: Boolean get() = username == null && password == null
}

/** The form rules every auth screen shares. */
object AuthValidation {

    /** Digits in the one-time code. */
    const val OTP_LENGTH = 6

    /** Shortest password we accept. */
    const val MIN_PASSWORD_LENGTH = 8

    /** How long the resend control stays locked after a code is sent. */
    const val OTP_RESEND_SECONDS = 30

    private const val MIN_NAME_LENGTH = 2
    private const val MIN_PHONE_DIGITS = 10
    private const val MIN_USERNAME_LENGTH = 4

    private val USERNAME_ALLOWED = Regex("^[A-Za-z0-9._]+$")
    private val EMAIL_ALLOWED = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateDetails(fullName: String, email: String, phoneNumber: String, dateOfBirth: String) = DetailsErrors(
        fullName = when {
            fullName.isBlank() -> "Enter your full name"
            fullName.trim().length < MIN_NAME_LENGTH -> "Enter your full name"
            else -> null
        },
        email = validateEmail(email),
        phoneNumber = validatePhoneNumberOptional(phoneNumber),
        dateOfBirth = validateDateOfBirthOptional(dateOfBirth)
    )

    fun validateDateOfBirthOptional(value: String): String? {
        if (value.isBlank()) return null
        val match = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").matchEntire(value.trim())
            ?: return "Enter a valid date of birth"
        val year = match.groupValues[1].toIntOrNull() ?: return "Enter a valid date of birth"
        val month = match.groupValues[2].toIntOrNull() ?: return "Enter a valid date of birth"
        val day = match.groupValues[3].toIntOrNull() ?: return "Enter a valid date of birth"
        val cal = java.util.Calendar.getInstance().apply {
            isLenient = false
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month - 1)
            set(java.util.Calendar.DAY_OF_MONTH, day)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return try {
            cal.time
            if (cal.get(java.util.Calendar.YEAR) != year ||
                cal.get(java.util.Calendar.MONTH) != month - 1 ||
                cal.get(java.util.Calendar.DAY_OF_MONTH) != day
            ) return "Enter a valid date of birth"
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            when {
                cal.after(today) -> "Date of birth cannot be in the future"
                year < 1900 -> "Enter a valid date of birth"
                else -> null
            }
        } catch (_: Exception) {
            "Enter a valid date of birth"
        }
    }

    fun validateEmail(value: String): String? = when {
        value.isBlank() -> "Enter your email address"
        !EMAIL_ALLOWED.matches(value) -> "Enter a valid email address"
        else -> null
    }

    fun validatePhoneNumber(value: String): String? = when {
        value.isBlank() -> "Enter your mobile number"
        value.count { it.isDigit() } < MIN_PHONE_DIGITS -> "Enter a complete mobile number"
        else -> null
    }

    /** Phone is optional on the details step: blank is fine, a partial number is not. */
    fun validatePhoneNumberOptional(value: String): String? = when {
        value.isBlank() -> null
        value.count { it.isDigit() } < MIN_PHONE_DIGITS -> "Enter a complete mobile number"
        else -> null
    }

    fun validateCredentials(
        username: String,
        password: String,
        confirmation: String
    ) = CredentialsErrors(
        username = validateUsername(username),
        password = validatePassword(password),
        confirmation = validateConfirmation(password, confirmation)
    )

    fun validateUsername(value: String): String? = when {
        value.isBlank() -> "Choose a username"
        value.length < MIN_USERNAME_LENGTH -> "Use at least $MIN_USERNAME_LENGTH characters"
        !USERNAME_ALLOWED.matches(value) -> "Letters, numbers, dots and underscores only"
        else -> null
    }

    fun validatePassword(value: String): String? =
        if (value.length < MIN_PASSWORD_LENGTH) {
            "Use at least $MIN_PASSWORD_LENGTH characters"
        } else {
            null
        }

    fun validateConfirmation(password: String, confirmation: String): String? = when {
        confirmation.isBlank() -> "Re-enter your password"
        confirmation != password -> "Passwords do not match"
        else -> null
    }

    fun validateOtp(code: String): String? = when {
        code.isBlank() -> "Enter the code we sent"
        code.length != OTP_LENGTH -> "Enter all $OTP_LENGTH digits"
        else -> null
    }

    /**
     * Sign-in only checks that both halves were typed. Which half was wrong is
     * the server's business, and saying so on screen would tell an attacker
     * which usernames exist.
     */
    fun validateSignIn(username: String, password: String) = SignInErrors(
        username = if (username.isBlank()) "Enter your username" else null,
        password = if (password.isBlank()) "Enter your password" else null
    )

    /**
     * The resend line: a countdown while the control is locked, and the name of
     * the action once the patient can ask for a new code.
     */
    fun resendLabel(secondsLeft: Int): String =
        if (secondsLeft > 0) "Resend code in ${secondsLeft}s" else "Resend code"
}
