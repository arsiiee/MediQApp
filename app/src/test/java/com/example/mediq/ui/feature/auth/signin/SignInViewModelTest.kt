package com.example.mediq.ui.feature.auth.signin

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.SignInRequest
import com.example.mediq.fake.FakeAuthRepository
import com.example.mediq.fake.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException

/**
 * Covers the sign-in form's state machine.
 *
 * `SignInViewModel` was untested while every other sign-in path was exercised end
 * to end on a device, and it holds two claims the rest of the app depends on:
 * that the credentials typed reach `AuthRepository.signIn` verbatim, and that a
 * refusal is shown as the sentence the server wrote rather than as a framework
 * message.
 *
 * The second one is the trap this file exists for. `AGENTS.md` records the day
 * `ApiFailure` was never mapped: Retrofit throws `HttpException`, whose
 * `message` is `"HTTP 401 "`, so every ViewModel surfacing `e.message` showed a
 * patient their own HTTP status. That shipped with a green build and a green
 * suite, because nothing here was asserted. [a wrong password shows the server's
 * sentence, not an HTTP status] is the guard.
 *
 * `e.message` is checked by asserting on the exact string the fake throws, so a
 * regression to `e.message` fails rather than passing on a generic fallback.
 */
class SignInViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: SignInViewModel

    private val username = "demo_patient"
    private val password = "demo12345"

    /** The sentence `AuthService` writes for both an unknown user and a bad password. */
    private val serverMessage = "Those details don't match an account."

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        viewModel = SignInViewModel(repository)
    }

    /** Fills both fields with valid credentials. */
    private fun fillValidForm() {
        viewModel.onUsernameChanged(username)
        viewModel.onPasswordChanged(password)
    }

    // --- Reaching the repository ---------------------------------------------

    @Test
    fun `a valid sign-in reaches the repository with the credentials typed`() {
        fillValidForm()

        viewModel.signIn()

        assertEquals(listOf(SignInRequest(username, password)), repository.signInRequests)
    }

    @Test
    fun `the credentials are sent exactly as typed, untrimmed`() {
        // Deliberately. `AuthService.normalizeUsername` trims and lowercases
        // server-side, so the client has no business doing it — a ViewModel that
        // normalised here would send something the user never typed, and the two
        // normalising sides would disagree about which string is canonical.
        viewModel.onUsernameChanged("  demo_patient  ")
        viewModel.onPasswordChanged("  demo12345  ")

        viewModel.signIn()

        assertEquals(
            SignInRequest("  demo_patient  ", "  demo12345  "),
            repository.signInRequests.single(),
        )
    }

    // --- Success --------------------------------------------------------------

    @Test
    fun `a successful sign-in reports itself signed in and is not loading`() {
        fillValidForm()

        viewModel.signIn()

        val state = viewModel.uiState.value
        assertTrue("expected the ViewModel to report a signed-in user", state.signedIn)
        assertFalse("a finished request is not still loading", state.isLoading)
        assertNull("success is not an error", state.error)
    }

    @Test
    fun `a failed sign-in does not report a signed-in user`() {
        fillValidForm()
        repository.signInError = ApiFailure(401, "invalid_credentials", serverMessage)

        viewModel.signIn()

        assertFalse(
            "a refused sign-in must not navigate the user to Home",
            viewModel.uiState.value.signedIn,
        )
    }

    @Test
    fun `a successful sign-in keeps the username and password in state`() {
        // The screen clears nothing on success, and the form is the only thing
        // holding a typed password — so if a future change blanks these, the
        // test says so rather than letting it happen silently.
        fillValidForm()

        viewModel.signIn()

        assertEquals(username, viewModel.uiState.value.username)
        assertEquals(password, viewModel.uiState.value.password)
    }

    // --- Client-side validation, before any request ---------------------------

    @Test
    fun `a blank username is rejected without contacting the server`() {
        viewModel.onUsernameChanged("   ")
        viewModel.onPasswordChanged(password)

        viewModel.signIn()

        assertEquals("Username and password are required.", viewModel.uiState.value.error)
        assertFalse("nothing to validate, so nothing to send", repository.signInWasCalled)
    }

    @Test
    fun `a blank password is rejected without contacting the server`() {
        viewModel.onUsernameChanged(username)
        viewModel.onPasswordChanged("")

        viewModel.signIn()

        assertEquals("Username and password are required.", viewModel.uiState.value.error)
        assertFalse(repository.signInWasCalled)
    }

    @Test
    fun `an empty form is rejected without contacting the server`() {
        viewModel.signIn()

        assertEquals("Username and password are required.", viewModel.uiState.value.error)
        assertFalse(repository.signInWasCalled)
        assertFalse("a rejected form is not loading", viewModel.uiState.value.isLoading)
    }

    @Test
    fun `a rejected form does not report a signed-in user`() {
        viewModel.signIn()

        assertFalse(
            "validation failure must never navigate to Home",
            viewModel.uiState.value.signedIn,
        )
    }

    // --- Failure surfaces the server's sentence -------------------------------

    @Test
    fun `a wrong password shows the server's sentence, not an HTTP status`() {
        fillValidForm()
        repository.signInError = ApiFailure(401, "invalid_credentials", serverMessage)

        viewModel.signIn()

        val error = viewModel.uiState.value.error
        assertEquals(serverMessage, error)
        // The exact failure this guards: `HttpException.message` is "HTTP 401 ",
        // which is what a patient saw before the error contract was mapped.
        assertFalse(
            "the framework's status string reached the user",
            error?.contains("HTTP") ?: false,
        )
    }

    @Test
    fun `a server refusal is not loading and keeps the typed credentials`() {
        fillValidForm()
        repository.signInError = ApiFailure(401, "invalid_credentials", serverMessage)

        viewModel.signIn()

        val state = viewModel.uiState.value
        assertFalse("a finished request is not still loading", state.isLoading)
        assertEquals(
            "the user should not have to retype a password they got right",
            password,
            state.password,
        )
    }

    @Test
    fun `an unexpected failure falls back to a message written for a person`() {
        fillValidForm()
        repository.signInError = IOException("failed to connect to /192.168.100.14:8099")

        viewModel.signIn()

        val error = viewModel.uiState.value.error
        assertNotNull("an unexpected failure must still say something", error)
        // An IOException's message is an OkHttp/host string. Showing it leaks
        // internals and tells the patient nothing actionable.
        assertFalse(
            "the framework's message reached the user",
            error?.contains("192.168.100.14") ?: false,
        )
        assertEquals(
            "Sign-in failed. Check your username and password.",
            error,
        )
    }

    // --- The error clears as the user corrects it ---------------------------

    @Test
    fun `editing the username clears a previous error`() {
        fillValidForm()
        repository.signInError = ApiFailure(401, "invalid_credentials", serverMessage)
        viewModel.signIn()
        assertNotNull("precondition: an error is showing", viewModel.uiState.value.error)

        viewModel.onUsernameChanged("demo_patient2")

        assertNull(
            "a stale refusal must not sit under a field the user is still editing",
            viewModel.uiState.value.error,
        )
    }

    @Test
    fun `editing the password clears a previous error`() {
        fillValidForm()
        repository.signInError = ApiFailure(401, "invalid_credentials", serverMessage)
        viewModel.signIn()

        viewModel.onPasswordChanged("demo123456")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `retrying after a refusal replaces the error rather than stacking it`() {
        fillValidForm()
        repository.signInError = ApiFailure(401, "invalid_credentials", serverMessage)
        viewModel.signIn()

        repository.signInError = null
        viewModel.signIn()

        val state = viewModel.uiState.value
        assertTrue(state.signedIn)
        assertNull("the old refusal must not survive a successful retry", state.error)
    }

    // --- Double submit -------------------------------------------------------

    @Test
    fun `a second submit while one is in flight does not reach the server twice`() {
        // The gate is what makes "in flight" real. `MainDispatcherRule` uses an
        // `UnconfinedTestDispatcher`, which runs a launch eagerly to completion,
        // so without it the first `signIn()` would have already finished by the
        // time the second one runs and this would assert nothing.
        val inFlight = CompletableDeferred<Unit>()
        repository.signInGate = inFlight
        fillValidForm()

        viewModel.signIn()
        assertTrue("precondition: the request is still in flight", viewModel.uiState.value.isLoading)

        viewModel.signIn()

        // The guard is `if (state.isLoading) return`, and this pins it. Without it
        // a double tap on the sign-in button sends two requests; with it, one.
        assertEquals(1, repository.signInRequests.size)

        inFlight.complete(Unit)
        assertTrue(viewModel.uiState.value.signedIn)
    }
}
