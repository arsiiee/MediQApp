package com.example.mediq.ui.feature.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mediq.di.AppContainer
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.RegisterRequest
import com.example.mediq.domain.model.RequestOtpRequest
import com.example.mediq.domain.model.VerifyOtpRequest
import com.example.mediq.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Which step the wizard has earned the right to move to.
 *
 * Held as a value the screen must explicitly clear, rather than as a sticky
 * `Boolean` per step. A flag stays true for the life of the ViewModel, so
 * `LaunchedEffect(otpRequested)` fires again every time the user navigates
 * *back* to that step — system back from the OTP screen would bounce them
 * straight forward to it again, with no way out of the wizard.
 */
enum class RegisterStep {
    OTP,
    CREDENTIALS,
    SUCCESS,
}

/**
 * The whole registration wizard's state, across all three steps.
 *
 * One state object rather than one per screen, because the steps are a single
 * transaction: the name and number collected on step 1 are still needed on step
 * 3. The screens used to keep them in local `remember` state, which discarded
 * them on Continue and meant no account could be created from the app at all.
 */
data class RegisterUiState(
    // Step 1 — details
    val fullName: String = "",
    val mobileNumber: String = "",
    val dateOfBirth: LocalDate? = null,

    // Step 2 — verification
    val otp: String = "",
    /**
     * The OTP echoed back by the server, which exists only because
     * `AuthService.returnCodeToCaller` is true for development. Shown as a
     * clearly-labelled hint; never required, since a real SMS provider sends no
     * code in the response.
     */
    val devOtpHint: String = "",

    // Step 3 — credentials
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    // Progress
    /**
     * True once the number has been proven. Doubles as the guard for step 3:
     * it can only be true while this ViewModel holds a registration id, which
     * is what `/auth/register` requires and what process death would lose.
     */
    val otpVerified: Boolean = false,

    val isLoading: Boolean = false,

    /** The name from the returned session, so step 4 greets a real person. */
    val registeredName: String? = null,

    /** Set when a step succeeds, cleared by the screen once it has navigated. */
    val pendingStep: RegisterStep? = null,

    val error: String? = null,
)

/**
 * Owns the three registration steps and the three API calls behind them.
 *
 * Scoped to a nested navigation graph rather than to any one screen, so all four
 * register destinations share this instance — see `MediQNavHost`.
 */
class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    /**
     * The server's proof that this phone number was verified. Deliberately not
     * in [RegisterUiState]: it belongs to no screen, and putting it in a state
     * object that Compose diffs would invite a route string to carry it instead.
     */
    private var registrationId: String? = null

    // --- Field changes -------------------------------------------------------

    fun onFullNameChanged(value: String) = edit { copy(fullName = value) }

    fun onMobileNumberChanged(value: String) = edit { copy(mobileNumber = value) }

    fun onDateOfBirthChanged(value: LocalDate?) = edit { copy(dateOfBirth = value) }

    fun onOtpChanged(value: String) = edit { copy(otp = value.filter(Char::isDigit).take(OTP_LENGTH)) }

    fun onUsernameChanged(value: String) = edit { copy(username = value) }

    fun onPasswordChanged(value: String) = edit { copy(password = value) }

    fun onConfirmPasswordChanged(value: String) = edit { copy(confirmPassword = value) }

    private inline fun edit(block: RegisterUiState.() -> RegisterUiState) {
        // Clearing the error as the user types stops a stale server message
        // hanging around next to a field they have already corrected.
        _uiState.value = _uiState.value.block().copy(error = null)
    }

    // --- Step 1: send a code -------------------------------------------------

    fun requestOtp() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.fullName.isBlank()) {
            fail("Enter your full name.")
            return
        }
        val mobile = sanitizeMobile(state.mobileNumber)
        if (!MOBILE_PATTERN.matches(mobile)) {
            fail("Enter a valid mobile number in international format, e.g. +639175550142.")
            return
        }
        // The server rejects a registration with no date of birth, and only
        // after several other checks — so it is asked for here instead.
        if (state.dateOfBirth == null) {
            fail("Enter your date of birth.")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val code = authRepository.requestOtp(RequestOtpRequest(mobile))
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    devOtpHint = code,
                    // A new code invalidates whatever the previous one proved.
                    otpVerified = false,
                    pendingStep = RegisterStep.OTP,
                )
                registrationId = null
            } catch (e: BackendNotConnectedException) {
                fail("Registration isn't available right now.")
            } catch (e: ApiFailure) {
                fail(e.message)
            } catch (e: Exception) {
                fail("Couldn't send a code. Please try again.")
            }
        }
    }

    // --- Step 2: verify the code --------------------------------------------

    fun verifyOtp() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.otp.length != OTP_LENGTH) {
            fail("Enter the $OTP_LENGTH-digit code we sent you.")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val id = authRepository.verifyOtp(
                    VerifyOtpRequest(
                        mobileNumber = sanitizeMobile(state.mobileNumber),
                        otp = state.otp,
                    )
                )
                registrationId = id
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    otpVerified = true,
                    pendingStep = RegisterStep.CREDENTIALS,
                )
            } catch (e: BackendNotConnectedException) {
                fail("Verification isn't available right now.")
            } catch (e: ApiFailure) {
                // The typed code is deliberately left in place: a wrong digit is
                // a re-type, not a re-request. The lockout case arrives here too
                // and shows its own sentence rather than a generic failure.
                fail(e.message)
            } catch (e: Exception) {
                fail("Couldn't verify that code. Please try again.")
            }
        }
    }

    // --- Step 3: create the account -----------------------------------------

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        // No verified number means no registration id, and `/auth/register`
        // cannot succeed without one. This is the process-death case, and it is
        // why `otpVerified` lives in the state rather than only the id.
        val id = registrationId
        if (id == null || !state.otpVerified) {
            fail("Verify your number before creating an account.")
            return
        }
        if (state.username.isBlank()) {
            fail("Enter your username.")
            return
        }
        if (state.password.length < MIN_PASSWORD_LENGTH) {
            fail("Use a password of at least $MIN_PASSWORD_LENGTH characters.")
            return
        }
        if (state.password != state.confirmPassword) {
            fail("Those passwords don't match.")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val session = authRepository.register(
                    RegisterRequest(
                        registrationId = id,
                        fullName = state.fullName.trim(),
                        username = state.username.trim(),
                        password = state.password,
                        // Non-null: `requestOtp` already refused to run without
                        // one, and the server rejects a null here as well.
                        dateOfBirth = state.dateOfBirth,
                        sex = null,
                        email = null,
                    )
                )
                // `RetrofitAuthRepository.register` has already stored this
                // session, so the user is signed in from here on.
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registeredName = session.profile.fullName,
                    pendingStep = RegisterStep.SUCCESS,
                )
            } catch (e: BackendNotConnectedException) {
                fail("Registration isn't available right now. No account was created.")
            } catch (e: ApiFailure) {
                // A duplicate username keeps `registrationId`, so fixing the
                // username retries without another code.
                fail(e.message)
            } catch (e: Exception) {
                fail("Couldn't create your account. Please try again.")
            }
        }
    }

    /**
     * Called by a screen once it has navigated, so returning to that step does
     * not navigate forward again.
     */
    fun onStepHandled() {
        _uiState.value = _uiState.value.copy(pendingStep = null)
    }

    private fun fail(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = message)
    }

    companion object {
        /** Mirrors `AuthService.MIN_PASSWORD_LENGTH`. */
        const val MIN_PASSWORD_LENGTH = 8

        private const val OTP_LENGTH = 6

        /**
         * Mirrors `AuthService.normalizeMobile`, which accepts `^\+?[0-9]{10,15}$`
         * and rejects anything with a space in it.
         */
        private val MOBILE_PATTERN = Regex("^\\+?[0-9]{10,15}$")

        /**
         * Drops the separators a phone keypad still allows. The field this value
         * comes from is where OTP delivery is addressed, so a mangled number
         * blocks the whole registration — and the server refuses a space.
         */
        private fun sanitizeMobile(raw: String): String =
            raw.filter { it.isDigit() || it == '+' }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RegisterViewModel(AppContainer.authRepository) }
        }
    }
}
