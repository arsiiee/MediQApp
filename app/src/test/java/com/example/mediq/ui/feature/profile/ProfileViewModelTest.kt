package com.example.mediq.ui.feature.profile

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.LoadState
import com.example.mediq.fake.FakeAuthRepository
import com.example.mediq.fake.FakeProfileRepository
import com.example.mediq.fake.MainDispatcherRule
import com.example.mediq.fake.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Covers the profile screen's state machine: read the stored session, and sign out.
 *
 * Two things here are worth more than the obvious ones.
 *
 * **[sign-out still navigates when the server refuses] is a deliberate choice,
 * not an accident.** `ProfileViewModel.signOut` wraps the server call in
 * `runCatching` and calls `onDone()` regardless, because the alternative is a
 * patient stranded on a signed-in screen with no way out — the token stays valid
 * server-side and the sign-out button just stops working. Navigating away is
 * recoverable; being stuck is not.
 *
 * **[an unconnected backend is a null session, not an error] follows the same
 * rule as the list reads.** `LoadState.Success(null)` reads as "nobody is signed
 * in", which is what the screen can act on, rather than an error banner over a
 * profile the user cannot change.
 *
 * ## What is deliberately *not* covered
 *
 * `ProfileViewModel`'s constructor takes a `ProfileRepository` and never reads it.
 * `ProfileScreen` calls only `refresh()` and `signOut()`, so no code path in the
 * app reaches `ProfileRepository.updateProfile` — the profile is display-only and
 * the field cannot be edited. Nothing here asserts that, because writing a test
 * to pin a dead constructor parameter would enshrine it as intended behaviour.
 * The repository itself is live and is guarded server-side by `ProfileUpdateTest`.
 */
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var authRepository: FakeAuthRepository

    /**
     * Built on first use, not in `setUp`.
     *
     * `init` loads the session, and `MainDispatcherRule`'s
     * `UnconfinedTestDispatcher` runs that launch eagerly to completion — so a
     * ViewModel constructed in `setUp` would have already settled before the test
     * set its fixture.
     */
    private val viewModel: ProfileViewModel by lazy {
        ProfileViewModel(profileRepository, authRepository)
    }

    @Before
    fun setUp() {
        profileRepository = FakeProfileRepository()
        authRepository = FakeAuthRepository()
    }

    // --- Reading the session --------------------------------------------------

    @Test
    fun `the stored session reaches the state`() {
        val session = TestFixtures.authSession()
        authRepository.currentSessionResult = session

        val loaded = (viewModel.uiState.value.session as? LoadState.Success)?.data

        assertEquals(session, loaded)
    }

    @Test
    fun `the session's real name is available, not a hardcoded placeholder`() {
        // `RegisterSuccessScreen` once hardcoded "Welcome to MediQ, Jesse." A
        // hardcoded personal name in a real-patient app is both a fabrication and
        // a data-integrity problem, so what reaches the state here is the name
        // the server actually sent.
        authRepository.currentSessionResult = TestFixtures.authSession(
            profile = TestFixtures.userProfile(fullName = "Maria Santos"),
        )

        val loaded = (viewModel.uiState.value.session as? LoadState.Success)?.data

        assertEquals("Maria Santos", loaded?.profile?.fullName)
    }

    @Test
    fun `nobody signed in is a successful null, not an error`() {
        authRepository.currentSessionResult = null

        val state = viewModel.uiState.value.session

        assertEquals(LoadState.Success(null), state)
    }

    @Test
    fun `an unconnected backend is a null session, not an error`() {
        authRepository.currentSessionError = BackendNotConnectedException()

        val state = viewModel.uiState.value.session

        assertEquals(
            "a null session is something the screen can act on; an error is a banner",
            LoadState.Success(null),
            state,
        )
    }

    @Test
    fun `a refused read is an error carrying the server's sentence`() {
        authRepository.currentSessionError =
            ApiFailure(401, "unauthorized", "Your session has ended. Sign in again.")

        val state = viewModel.uiState.value.session

        assertEquals(
            "Your session has ended. Sign in again.",
            (state as? LoadState.Error)?.message,
        )
    }

    @Test
    fun `an unexpected read failure falls back to a message written for a person`() {
        authRepository.currentSessionError = IOException("failed to connect to /10.0.2.2:8099")

        val state = viewModel.uiState.value.session

        assertEquals(
            "Couldn't load your profile. Try again in a moment.",
            (state as? LoadState.Error)?.message,
        )
    }

    // --- Refreshing -----------------------------------------------------------

    @Test
    fun `a refresh re-reads the session`() {
        authRepository.currentSessionResult = TestFixtures.authSession(
            profile = TestFixtures.userProfile(fullName = "Before"),
        )
        assertEquals("Before", loadedName())

        authRepository.currentSessionResult = TestFixtures.authSession(
            profile = TestFixtures.userProfile(fullName = "After"),
        )
        viewModel.refresh()

        assertEquals("After", loadedName())
    }

    @Test
    fun `a refresh recovers from a previous error`() {
        authRepository.currentSessionError = ApiFailure(500, "internal_error", "Something went wrong.")
        assertTrue("precondition: an error is showing", viewModel.uiState.value.session is LoadState.Error)

        authRepository.currentSessionError = null
        authRepository.currentSessionResult = TestFixtures.authSession()
        viewModel.refresh()

        assertTrue(viewModel.uiState.value.session is LoadState.Success)
    }

    // --- Signing out ----------------------------------------------------------

    @Test
    fun `signing out reaches the server`() {
        var navigated = false
        viewModel.uiState.value // constructed, so `init` has settled

        viewModel.signOut { navigated = true }

        assertTrue("the sign-out call never left the ViewModel", authRepository.signOutWasCalled)
        assertTrue("the screen was never told to navigate away", navigated)
    }

    @Test
    fun `signing out navigates away even when the server refuses`() {
        // The whole point of the `runCatching`. Without it, a failed sign-out
        // strands the patient on a signed-in screen whose button does nothing,
        // and the token stays valid server-side.
        authRepository.signOutError = ApiFailure(500, "internal_error", "Something went wrong.")
        var navigated = false
        viewModel.uiState.value

        viewModel.signOut { navigated = true }

        assertTrue(
            "a user must never be stuck on a screen they cannot leave",
            navigated,
        )
    }

    @Test
    fun `signing out navigates away when the server is unreachable`() {
        authRepository.signOutError = IOException("failed to connect to /10.0.2.2:8099")
        var navigated = false
        viewModel.uiState.value

        viewModel.signOut { navigated = true }

        assertTrue(navigated)
    }

    @Test
    fun `signing out navigates exactly once`() {
        var navigations = 0
        viewModel.uiState.value

        viewModel.signOut { navigations++ }

        assertEquals(1, navigations)
    }

    @Test
    fun `signing out does not put an error in the profile state`() {
        // The screen is navigating away; an error there would flash a banner on
        // the way to the sign-in screen for a failure the user cannot act on.
        authRepository.signOutError = ApiFailure(500, "internal_error", "Something went wrong.")
        viewModel.uiState.value

        viewModel.signOut { }

        assertNotNull("precondition: a session was loaded", viewModel.uiState.value.session)
        assertFalse(
            "a failed sign-out must not blank the profile on the way out",
            viewModel.uiState.value.session is LoadState.Error,
        )
    }

    @Test
    fun `signing out does not touch the profile repository`() {
        // Pins the dead-constructor-parameter claim above at the boundary where it
        // is true. If someone wires an edit form to this ViewModel later, this
        // fails and points at the test that needs updating deliberately.
        viewModel.uiState.value

        viewModel.signOut { }

        assertFalse(
            "signing out is an auth action, not a profile update",
            profileRepository.wasMutated,
        )
        assertNull(authRepository.currentSessionResult)
    }

    private fun loadedName(): String? =
        ((viewModel.uiState.value.session as? LoadState.Success)?.data as? AuthSession)
            ?.profile?.fullName
}
