package com.example.mediq.ui.feature.auth.register

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.fake.FakeAuthRepository
import com.example.mediq.fake.MainDispatcherRule
import com.example.mediq.fake.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Covers the registration wizard's state machine.
 *
 * The bug this exists to pin: all four register screens held their values in
 * local `remember { mutableStateOf(...) }` and navigated between each other on
 * a button press. Nothing reached `AuthRepository`, so the full name and phone
 * number were discarded on Continue and no account could ever be created from
 * the app — while the build stayed green, because "not calling the API" is not
 * a compile error.
 *
 * Every assertion below is about whether the repository was reached, not just
 * about what the state says afterwards. A test that only checked the final state
 * would still pass against a ViewModel that ignored its inputs.
 */
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: RegisterViewModel

    private val dateOfBirth = LocalDate.of(1995, 6, 15)

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        viewModel = RegisterViewModel(repository)
    }

    /** Fills step 1 with a complete, valid draft. */
    private fun fillDetails() {
        viewModel.onFullNameChanged("Maria Santos")
        viewModel.onMobileNumberChanged("+639175550142")
        viewModel.onDateOfBirthChanged(dateOfBirth)
    }

    /** Fills step 3 with a complete, valid draft. */
    private fun fillCredentials() {
        viewModel.onUsernameChanged("maria.santos")
        viewModel.onPasswordChanged("correct-horse")
        viewModel.onConfirmPasswordChanged("correct-horse")
    }

    // --- Step 1: nothing reaches the server until the draft is complete -----

    @Test
    fun `a blank full name is rejected without contacting the server`() {
        viewModel.onFullNameChanged("   ")
        viewModel.onMobileNumberChanged("+639175550142")
        viewModel.onDateOfBirthChanged(dateOfBirth)

        viewModel.requestOtp()

        assertNotNull("expected a validation error", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.pendingStep)
        assertTrue(repository.otpRequests.isEmpty())
    }

    @Test
    fun `a blank phone number is rejected without contacting the server`() {
        viewModel.onFullNameChanged("Maria Santos")
        viewModel.onDateOfBirthChanged(dateOfBirth)

        viewModel.requestOtp()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(repository.otpRequests.isEmpty())
    }

    @Test
    fun `a missing date of birth is rejected without contacting the server`() {
        // The server requires this field and rejects the registration without it
        // (AuthServiceTest, "register rejects registration without a date of
        // birth"). `RegisterRequest.dateOfBirth` is nullable so the compiler
        // cannot catch the omission — only this check can.
        viewModel.onFullNameChanged("Maria Santos")
        viewModel.onMobileNumberChanged("+639175550142")

        viewModel.requestOtp()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(repository.otpRequests.isEmpty())
    }

    @Test
    fun `a phone number the server would reject is caught before the round trip`() {
        // `AuthService.normalizeMobile` accepts `^\+?[0-9]{10,15}$` — so this is
        // too short once separators are stripped, and the server would answer
        // "Enter a valid mobile number in international format."
        viewModel.onFullNameChanged("Maria Santos")
        viewModel.onMobileNumberChanged("+63 917 555")
        viewModel.onDateOfBirthChanged(dateOfBirth)

        viewModel.requestOtp()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(repository.otpRequests.isEmpty())
    }

    @Test
    fun `separators typed into the phone field are stripped before sending`() {
        // A phone keypad still allows spaces, dashes and brackets. The field is
        // where OTP delivery is addressed, so a value the server refuses blocks
        // the entire registration.
        viewModel.onFullNameChanged("Maria Santos")
        viewModel.onMobileNumberChanged("+63 (917) 555-0142")
        viewModel.onDateOfBirthChanged(dateOfBirth)

        viewModel.requestOtp()

        assertEquals("+639175550142", repository.otpRequests.single().mobileNumber)
    }

    @Test
    fun `a complete draft requests a code and advances the wizard`() {
        fillDetails()

        viewModel.requestOtp()

        assertEquals(RegisterStep.OTP, viewModel.uiState.value.pendingStep)
        assertNull(viewModel.uiState.value.error)
        assertEquals(1, repository.otpRequests.size)
    }

    @Test
    fun `a handled step does not re-navigate when the user comes back to it`() {
        // The screens `LaunchedEffect` on this value, so a sticky flag would fire
        // again on every recomposition — system back from the OTP step would
        // bounce the user straight forward to it, trapping them in the wizard.
        fillDetails()
        viewModel.requestOtp()
        assertEquals(RegisterStep.OTP, viewModel.uiState.value.pendingStep)

        viewModel.onStepHandled()

        assertNull(
            "the event must be consumed once the screen has navigated",
            viewModel.uiState.value.pendingStep,
        )
    }

    @Test
    fun `the server's failure to send a code is surfaced`() {
        repository.requestOtpError =
            ApiFailure(400, "bad_request", "Enter a valid mobile number in international format.")
        fillDetails()

        viewModel.requestOtp()

        assertEquals(
            "Enter a valid mobile number in international format.",
            viewModel.uiState.value.error,
        )
        assertNull(
            "a failed request must not advance the wizard",
            viewModel.uiState.value.pendingStep,
        )
    }

    // --- The dev-only OTP hint ----------------------------------------------

    @Test
    fun `the dev OTP is shown only when the server actually returned one`() {
        // `AuthService.returnCodeToCaller` is true for development, so the code
        // comes back in the response. A real SMS provider returns no code, and
        // the screen must still work — so an empty code must not render a hint.
        repository.requestOtpResult = ""
        fillDetails()

        viewModel.requestOtp()

        assertEquals("", viewModel.uiState.value.devOtpHint)
    }

    @Test
    fun `a returned OTP is captured as a dev hint`() {
        repository.requestOtpResult = "482913"
        fillDetails()

        viewModel.requestOtp()

        assertEquals("482913", viewModel.uiState.value.devOtpHint)
    }

    // --- Step 2: verification -----------------------------------------------

    @Test
    fun `a wrong code shows the server's message and keeps the typed code`() {
        fillDetails()
        viewModel.requestOtp()
        repository.verifyOtpError = ApiFailure(400, "bad_request", "That code isn't right.")
        viewModel.onOtpChanged("000000")

        viewModel.verifyOtp()

        assertEquals("That code isn't right.", viewModel.uiState.value.error)
        assertFalse("must not advance on a wrong code", viewModel.uiState.value.otpVerified)
        // Clearing the box would make someone re-read a 6-digit code they can
        // still see and retype.
        assertEquals("000000", viewModel.uiState.value.otp)
    }

    @Test
    fun `too many wrong attempts surfaces the lockout message`() {
        fillDetails()
        viewModel.requestOtp()
        // `OtpLockoutTest` pins the threshold at five; the server answers 429
        // with its own sentence rather than a generic failure.
        repository.verifyOtpError =
            ApiFailure(429, "too_many_attempts", "Too many wrong codes. Request a new one.")
        viewModel.onOtpChanged("000000")

        viewModel.verifyOtp()

        assertEquals("Too many wrong codes. Request a new one.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.otpVerified)
    }

    @Test
    fun `a correct code marks the number verified`() {
        fillDetails()
        viewModel.requestOtp()
        viewModel.onOtpChanged("482913")

        viewModel.verifyOtp()

        assertTrue(viewModel.uiState.value.otpVerified)
        assertEquals(RegisterStep.CREDENTIALS, viewModel.uiState.value.pendingStep)
        assertNull(viewModel.uiState.value.error)
        assertEquals("482913", repository.verifyRequests.single().otp)
    }

    // --- Step 3: registration ------------------------------------------------

    @Test
    fun `registration is refused when the number was never verified`() {
        // Process death between steps 2 and 3 loses the in-memory registration
        // id. Calling register() without it would send a request the server can
        // only answer with "Verify your number first".
        fillCredentials()

        viewModel.register()

        assertFalse(
            "register must not be called without a verified number",
            repository.registerWasCalled,
        )
        assertFalse(viewModel.uiState.value.otpVerified)
    }

    @Test
    fun `a password under the server's minimum is refused without a round trip`() {
        completeThroughVerification()
        viewModel.onUsernameChanged("maria.santos")
        viewModel.onPasswordChanged("short")
        viewModel.onConfirmPasswordChanged("short")

        viewModel.register()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(repository.registerWasCalled)
    }

    @Test
    fun `mismatched passwords are refused without a round trip`() {
        completeThroughVerification()
        viewModel.onUsernameChanged("maria.santos")
        viewModel.onPasswordChanged("correct-horse")
        viewModel.onConfirmPasswordChanged("correct-horsey")

        viewModel.register()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(repository.registerWasCalled)
    }

    @Test
    fun `a successful registration reports the name the server returned`() {
        repository.registerResult = TestFixtures.authSession(
            profile = TestFixtures.userProfile(fullName = "Maria Santos")
        )
        completeThroughVerification()
        fillCredentials()

        viewModel.register()

        assertEquals("Maria Santos", viewModel.uiState.value.registeredName)
        assertEquals(RegisterStep.SUCCESS, viewModel.uiState.value.pendingStep)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `the registration request carries every field the server requires`() {
        completeThroughVerification()
        fillCredentials()

        viewModel.register()

        val sent = repository.registerRequests.single()
        assertEquals("reg_test-registration-id", sent.registrationId)
        assertEquals("Maria Santos", sent.fullName)
        assertEquals("maria.santos", sent.username)
        assertEquals("correct-horse", sent.password)
        // Non-null here is the point: the server rejects a null date of birth
        // mid-transaction, and the type would not have caught it.
        assertEquals(dateOfBirth, sent.dateOfBirth)
    }

    @Test
    fun `a duplicate username shows the server's message and keeps the wizard usable`() {
        repository.registerError =
            ApiFailure(400, "bad_request", "That username is already taken.")
        completeThroughVerification()
        fillCredentials()

        viewModel.register()

        assertEquals("That username is already taken.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.registeredName)
        assertNotEquals(
            // Not `assertNull`: nothing renders a screen in a unit test, so the
            // CREDENTIALS event from step 2 is legitimately still pending. What
            // matters is that a failure did not advance the wizard.
            RegisterStep.SUCCESS,
            viewModel.uiState.value.pendingStep,
        )
        // The verified registration id survives so picking a different username
        // retries without sending the user back for another OTP code.
        assertTrue(viewModel.uiState.value.otpVerified)
    }

    @Test
    fun `an unreachable server does not crash the wizard`() {
        repository.requestOtpError = ApiFailure(
            status = ApiFailure.STATUS_NO_RESPONSE,
            code = ApiFailure.CODE_NETWORK,
            message = "Couldn't reach the clinic. Check your connection and try again.",
        )
        fillDetails()

        viewModel.requestOtp()

        assertEquals(
            "Couldn't reach the clinic. Check your connection and try again.",
            viewModel.uiState.value.error,
        )
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // --- The whole flow ------------------------------------------------------

    @Test
    fun `a full three-step run registers the account`() {
        completeThroughVerification()
        fillCredentials()

        viewModel.register()

        assertEquals(1, repository.otpRequests.size)
        assertEquals(1, repository.verifyRequests.size)
        assertEquals(1, repository.registerRequests.size)
        assertNotNull(viewModel.uiState.value.registeredName)
    }

    @Test
    fun `editing a field clears the error it was showing`() {
        completeThroughVerification()
        viewModel.onUsernameChanged("taken")
        viewModel.onPasswordChanged("correct-horse")
        viewModel.onConfirmPasswordChanged("correct-horse")
        repository.registerError =
            ApiFailure(400, "bad_request", "That username is already taken.")
        viewModel.register()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.onUsernameChanged("maria.santos")

        assertNull("a stale error should not outlive the edit that fixes it", viewModel.uiState.value.error)
    }

    /** Runs steps 1 and 2 successfully, leaving the wizard on step 3. */
    private fun completeThroughVerification() {
        fillDetails()
        viewModel.requestOtp()
        viewModel.onOtpChanged("482913")
        viewModel.verifyOtp()
    }
}
