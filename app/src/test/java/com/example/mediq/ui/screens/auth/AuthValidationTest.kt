package com.example.mediq.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * The auth form rules are pure data, so they are tested here rather than
 * through a device: what each field accepts, and what the code step says while
 * its resend control is locked.
 */
class AuthValidationTest {

    // Details

    @Test
    fun `a name and a complete number are accepted`() {
        val errors = AuthValidation.validateDetails("Kissie Ann Apple", "+63 917 555 0142")

        assertTrue(errors.isClean)
    }

    @Test
    fun `an empty form flags both fields`() {
        val errors = AuthValidation.validateDetails("", "")

        assertEquals("Enter your full name", errors.fullName)
        assertEquals("Enter your mobile number", errors.phoneNumber)
        assertFalse(errors.isClean)
    }

    @Test
    fun `the number is judged on its digits, not on how it is written`() {
        assertNull(AuthValidation.validatePhoneNumber("0917-555-0142"))
        assertNull(AuthValidation.validatePhoneNumber("+63 (917) 555 0142"))
        assertEquals(
            "Enter a complete mobile number",
            AuthValidation.validatePhoneNumber("0917")
        )
    }

    // Credentials

    @Test
    fun `a username needs four allowed characters`() {
        assertEquals("Choose a username", AuthValidation.validateUsername(""))
        assertEquals("Use at least 4 characters", AuthValidation.validateUsername("ki"))
        assertEquals(
            "Letters, numbers, dots and underscores only",
            AuthValidation.validateUsername("kissie apple")
        )
        assertNull(AuthValidation.validateUsername("kissie.apple"))
    }

    @Test
    fun `a password has to reach the minimum length`() {
        assertNotNull(AuthValidation.validatePassword("short"))
        assertNull(AuthValidation.validatePassword("longenough"))
    }

    @Test
    fun `the confirmation has to match, and cannot be left blank`() {
        assertEquals(
            "Re-enter your password",
            AuthValidation.validateConfirmation("longenough", "")
        )
        assertEquals(
            "Passwords do not match",
            AuthValidation.validateConfirmation("longenough", "longenough2")
        )
        assertNull(AuthValidation.validateConfirmation("longenough", "longenough"))
    }

    @Test
    fun `credentials are clean only when all three fields pass`() {
        assertTrue(
            AuthValidation
                .validateCredentials("kissie.apple", "longenough", "longenough")
                .isClean
        )
        assertFalse(
            AuthValidation
                .validateCredentials("ki", "short", "other")
                .isClean
        )
    }

    // One-time code

    @Test
    fun `the code has to be six digits`() {
        assertEquals("Enter the code we sent", AuthValidation.validateOtp(""))
        assertEquals("Enter all 6 digits", AuthValidation.validateOtp("123"))
        assertNull(AuthValidation.validateOtp("123456"))
    }

    @Test
    fun `the resend line counts down and then names the action`() {
        assertEquals("Resend code in 30s", AuthValidation.resendLabel(30))
        assertEquals("Resend code in 1s", AuthValidation.resendLabel(1))
        assertEquals("Resend code", AuthValidation.resendLabel(0))
    }

    // Sign-in

    @Test
    fun `sign-in only asks that both halves were typed`() {
        val blank = AuthValidation.validateSignIn("", "")

        assertEquals("Enter your username", blank.username)
        assertEquals("Enter your password", blank.password)
        assertFalse(blank.isClean)

        // Length rules belong to registration; the server judges the pair here.
        assertTrue(AuthValidation.validateSignIn("k", "p").isClean)
    }

    // Samples

    @Test
    fun `the samples show every field of their form`() {
        assertNotNull(AuthSamples.detailsErrors.fullName)
        assertNotNull(AuthSamples.detailsErrors.phoneNumber)

        assertNotNull(AuthSamples.credentialsErrors.username)
        assertNotNull(AuthSamples.credentialsErrors.password)
        assertNotNull(AuthSamples.credentialsErrors.confirmation)

        assertNotNull(AuthSamples.signInErrors.username)
        assertNotNull(AuthSamples.signInErrors.password)
        assertNotNull(AuthSamples.otpIncomplete)
    }
}
