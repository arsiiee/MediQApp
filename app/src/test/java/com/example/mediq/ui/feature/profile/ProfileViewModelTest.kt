package com.example.mediq.ui.feature.profile

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.UserRole
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
import java.time.LocalDate

/**
 * Covers the profile screen's state machine: read the stored session, save an
 * edit, and sign out.
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
 * ## The edit path
 *
 * The form is prefilled from the loaded session, and a save merges the typed
 * values over that profile — so the fields the form does not carry (id,
 * username, role) cannot be blanked by a save, and an untouched field keeps its
 * value rather than arriving empty.
 *
 * The state is then rewritten from the **server's** response, not from what was
 * typed: the server normalises, and its answer is what the profile screen must
 * show. `FakeAuthRepository.cachedProfileUpdates` is the assertion that the
 * store `currentSession()` actually reads was rewritten — without that write the
 * screen would keep showing the old name until the next sign-in, which no
 * state-only check would catch.
 *
 * Validation is pinned as "never reaches the network", not "shows a message":
 * a field error that still fires a request is a request the server has to
 * refuse, and the patient waits for it.
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
        // Sign-out is an auth action. Editing now exists on this ViewModel, so
        // this is the boundary that keeps the two apart: a sign-out that wrote a
        // profile would be an update nobody asked for.
        viewModel.uiState.value

        viewModel.signOut { }

        assertFalse(
            "signing out is an auth action, not a profile update",
            profileRepository.wasMutated,
        )
        assertNull(authRepository.currentSessionResult)
    }

    @Test
    fun `signing out followed by signing in restores session cleanly`() {
        var navigatedToSignIn = false
        viewModel.signOut { navigatedToSignIn = true }
        assertTrue(navigatedToSignIn)

        // Simulate subsequent sign-in on the auth repository
        authRepository.currentSessionResult = TestFixtures.authSession()
        viewModel.refresh()

        val state = viewModel.uiState.value.session
        assertTrue(state is LoadState.Success)
        assertNotNull((state as LoadState.Success).data)
    }

    private fun loadedName(): String? =
        ((viewModel.uiState.value.session as? LoadState.Success)?.data as? AuthSession)
            ?.profile?.fullName

    // --- Editing --------------------------------------------------------------

    @Test
    fun `the edit form starts prefilled from the session`() {
        // A blank form would let a save overwrite fields the patient never saw.
        authRepository.currentSessionResult = TestFixtures.authSession(
            profile = TestFixtures.userProfile(fullName = "Maria Santos", email = "maria@example.com"),
        )

        val edit = viewModel.uiState.value.edit

        assertEquals("Maria Santos", edit.fullName)
        assertEquals("maria@example.com", edit.email)
        assertEquals("+639175550142", edit.mobileNumber)
        assertEquals("1995-06-15", edit.dateOfBirth)
    }

    @Test
    fun `saving sends the edited fields, merged over the current profile`() {
        signedIn()

        viewModel.onEditFullName("Renamed")
        viewModel.onEditEmail("renamed@example.com")
        viewModel.onEditAddress("9 New Road")
        viewModel.saveProfile { }

        val sent = profileRepository.updateRequests.single()
        assertEquals("Renamed", sent.fullName)
        assertEquals("renamed@example.com", sent.email)
        assertEquals("9 New Road", sent.address)
        assertEquals("a save is an edit, not a new record", "user-1", sent.id)
        assertEquals("maria.santos", sent.username)
        assertEquals(UserRole.PATIENT, sent.role)
        assertEquals(
            "a field the form never showed must keep its value",
            "+639175550142",
            sent.mobileNumber,
        )
    }

    @Test
    fun `whitespace around the typed fields is trimmed before the server sees it`() {
        signedIn()

        viewModel.onEditFullName("  Renamed  ")
        viewModel.onEditEmail("  renamed@example.com  ")
        viewModel.saveProfile { }

        val sent = profileRepository.updateRequests.single()
        assertEquals("Renamed", sent.fullName)
        assertEquals("renamed@example.com", sent.email)
    }

    @Test
    fun `a blank address is sent as absent, not as an empty string`() {
        signedIn()

        viewModel.onEditAddress("   ")
        viewModel.saveProfile { }

        assertNull(profileRepository.updateRequests.single().address)
    }

    @Test
    fun `a successful save rewrites the cached session with the server's answer`() {
        signedIn()
        // The server normalises. Its response, not the typed value, is what the
        // profile screen must show afterwards.
        profileRepository.updateResult = TestFixtures.userProfile(fullName = "Maria Santos-Reyes")

        viewModel.onEditFullName("maria santos-reyes")
        viewModel.saveProfile { }

        assertEquals(1, authRepository.cachedProfileUpdates.size)
        assertEquals("Maria Santos-Reyes", authRepository.cachedProfileUpdates.single().fullName)
        assertEquals(
            "the screen must render the server's answer",
            "Maria Santos-Reyes",
            loadedName(),
        )
    }

    @Test
    fun `a successful save calls the screen's finish callback`() {
        signedIn()
        var finished = 0

        viewModel.onEditFullName("Renamed")
        viewModel.saveProfile { finished++ }

        assertEquals(1, finished)
    }

    @Test
    fun `a refused save shows the server's sentence and keeps the form`() {
        signedIn()
        profileRepository.updateProfileError = ApiFailure(
            status = 400,
            code = "bad_request",
            message = "That email is already used by another account.",
        )
        var finished = 0

        viewModel.onEditFullName("Renamed")
        viewModel.saveProfile { finished++ }

        val edit = viewModel.uiState.value.edit
        assertEquals("That email is already used by another account.", edit.saveError)
        assertEquals("Renamed", edit.fullName)
        assertFalse("a refused save must not look like a success", edit.isSaving)
        assertEquals("the screen must not navigate away on a failure", 0, finished)
    }

    @Test
    fun `an unreachable backend says the change was not saved`() {
        signedIn()
        profileRepository.updateProfileError = IOException("failed to connect to /10.0.2.2:8099")

        viewModel.onEditFullName("Renamed")
        viewModel.saveProfile { }

        assertEquals(
            "Couldn't save your changes. Try again in a moment.",
            viewModel.uiState.value.edit.saveError,
        )
    }

    @Test
    fun `a blank full name never reaches the network`() {
        signedIn()

        viewModel.onEditFullName("   ")
        viewModel.saveProfile { }

        assertNotNull(viewModel.uiState.value.edit.fullNameError)
        assertTrue(profileRepository.updateRequests.isEmpty())
    }

    @Test
    fun `a malformed date of birth never reaches the network`() {
        signedIn()

        viewModel.onEditDateOfBirth("15/06/1995")
        viewModel.saveProfile { }

        assertNotNull(viewModel.uiState.value.edit.dateOfBirthError)
        assertTrue(profileRepository.updateRequests.isEmpty())
    }

    @Test
    fun `a date of birth in the future never reaches the network`() {
        signedIn()

        viewModel.onEditDateOfBirth(LocalDate.now().plusDays(1).toString())
        viewModel.saveProfile { }

        assertNotNull(viewModel.uiState.value.edit.dateOfBirthError)
        assertTrue(profileRepository.updateRequests.isEmpty())
    }

    @Test
    fun `an invalid email and mobile number are refused before the network`() {
        signedIn()

        viewModel.onEditEmail("not-an-email")
        viewModel.onEditMobile("123")
        viewModel.saveProfile { }

        val edit = viewModel.uiState.value.edit
        assertNotNull(edit.emailError)
        assertNotNull(edit.mobileError)
        assertTrue(profileRepository.updateRequests.isEmpty())
    }

    @Test
    fun `changing a field clears its own error`() {
        signedIn()
        viewModel.onEditFullName("   ")
        viewModel.saveProfile { }
        assertNotNull("precondition: the field is showing an error", viewModel.uiState.value.edit.fullNameError)

        viewModel.onEditFullName("Renamed")

        assertNull(viewModel.uiState.value.edit.fullNameError)
    }

    @Test
    fun `nobody signed in cannot save`() {
        // Reachable in principle the moment the screen is deep-linked: a save
        // with no session has no id to update and must not be attempted.
        authRepository.currentSessionResult = null

        viewModel.onEditFullName("Renamed")
        viewModel.saveProfile { }

        assertTrue(profileRepository.updateRequests.isEmpty())
        assertFalse(authRepository.cachedProfileWasUpdated)
    }

    private fun signedIn() {
        authRepository.currentSessionResult = TestFixtures.authSession()
    }
}
