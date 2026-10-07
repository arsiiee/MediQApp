package com.example.mediq.ui.feature.doctors

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Specialty
import com.example.mediq.fake.FakeDoctorRepository
import com.example.mediq.fake.TestFixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Covers the doctor list's search and filter behaviour.
 *
 * ## Why this file does not use `MainDispatcherRule`
 *
 * The shared rule installs an `UnconfinedTestDispatcher`, which runs a launch
 * eagerly to completion. That is the right default — the other ViewModel tests
 * assert on settled state and would otherwise have to `advanceUntilIdle()` first
 * — but it makes `delay` untestable: a debounce is *nothing happening for 300 ms
 * and then something happening*, and an eager dispatcher collapses the wait
 * before a test can observe it.
 *
 * So this file owns its dispatcher: a `StandardTestDispatcher` on a scheduler the
 * test drives by hand. Every assertion about timing here is about how far the
 * clock has been moved, which makes the timing itself part of what is under test
 * rather than an accident of the harness.
 *
 * ## What is worth pinning
 *
 * **Typing must not fire a request per keystroke.** The patient search field is
 * wired to `onSearchTextChange` on every character, and each one would otherwise
 * be a server round trip. [typing does not query before the debounce elapses] and
 * [a burst of keystrokes produces exactly one query] are the two halves of that
 * claim.
 *
 * **The debounce restarts, it does not queue.** A second keystroke after 250 ms
 * must delay the request again rather than let the first timer fire — otherwise a
 * patient typing a six-letter surname fires two requests and can see the
 * six-letter results replaced by the single-letter ones.
 *
 * **Changing the filter is not debounced.** A chip press is a deliberate act, and
 * making the patient wait 300 ms to see what they just asked for is a bug.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DoctorsViewModelTest {

    private val testScheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private val testScope = TestScope(testScheduler)

    private lateinit var repository: FakeDoctorRepository

    @Before
    fun setUp() {
        repository = FakeDoctorRepository()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = DoctorsViewModel(repository)

    private fun doctorsOf(state: DoctorsUiState) =
        (state.doctors as? LoadState.Success)?.data

    // --- Opening the screen fetches ------------------------------------------

    @Test
    fun `opening the screen queries the list`() = testScope.runTest {
        repository.doctors = listOf(TestFixtures.doctor(fullName = "Rivera"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(
            "a patient who opens Doctors must not be left looking at a spinner forever",
            listOf(null),
            repository.requestedQueries.map { it.searchText },
        )
        assertEquals(
            listOf("Rivera"),
            doctorsOf(viewModel.uiState.value)?.map { it.fullName },
        )
    }

    @Test
    fun `an initial response arriving after a search cannot replace the search results`() = testScope.runTest {
        val initialRead = CompletableDeferred<Unit>()
        repository.getDoctorsGate = initialRead
        val viewModel = viewModel()
        runCurrent() // initial read is suspended inside the fake

        repository.getDoctorsGate = null
        repository.doctors = listOf(TestFixtures.doctor(fullName = "Filtered Rivera"))
        viewModel.onSearchTextChange("Rivera")
        advanceTimeBy(300)
        advanceUntilIdle()
        assertEquals(listOf(null, "Rivera"), repository.requestedQueries.map { it.searchText })
        assertEquals(listOf("Filtered Rivera"), doctorsOf(viewModel.uiState.value)?.map { it.fullName })

        repository.doctors = listOf(TestFixtures.doctor(fullName = "Stale unfiltered"))
        initialRead.complete(Unit)
        advanceUntilIdle()
        assertEquals(
            "a late opening response must not overwrite a newer filtered result",
            listOf("Filtered Rivera"),
            doctorsOf(viewModel.uiState.value)?.map { it.fullName },
        )
    }

    @Test
    fun `typing invalidates an initial read even before the debounce has elapsed`() = testScope.runTest {
        val initialRead = CompletableDeferred<Unit>()
        repository.getDoctorsGate = initialRead
        val viewModel = viewModel()
        runCurrent()

        viewModel.onSearchTextChange("Rivera")
        repository.doctors = listOf(TestFixtures.doctor(fullName = "Stale unfiltered"))
        initialRead.complete(Unit)
        runCurrent()
        assertTrue(
            "do not briefly show all doctors for a query the patient already typed",
            viewModel.uiState.value.doctors is LoadState.Loading,
        )
    }

    /**
     * A screen whose ViewModel has already run its initial load, with the query
     * log cleared.
     *
     * Every test below this point is about what the *patient's* next action does
     * — a keystroke, a chip press — and each asserts on an exact query count.
     * Without clearing the log the opening load would sit in front of it as
     * `requestedQueries[0]` and every count would be off by one, so the tests
     * would have to be rewritten to tolerate it. Clearing it instead keeps each
     * assertion reading exactly as it did before: given a loaded screen, when the
     * patient types, then …
     */
    private suspend fun TestScope.loadedViewModel(): DoctorsViewModel {
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.requestedQueries.clear()
        return viewModel
    }

    @Test
    fun `an explicit refresh queries straight away`() = testScope.runTest {
        val viewModel = loadedViewModel()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.requestedQueries.size)
    }

    // --- The debounce ---------------------------------------------------------

    @Test
    fun `typing does not query before the debounce elapses`() = testScope.runTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchTextChange("Riv")
        advanceTimeBy(299)

        assertFalse(
            "299 ms is short of the 300 ms debounce; a request here means one per keystroke",
            repository.wasQueriedAtAll,
        )
    }

    @Test
    fun `typing queries once the debounce elapses`() = testScope.runTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchTextChange("Riv")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(listOf("Riv"), repository.requestedQueries.map { it.searchText })
    }

    @Test
    fun `a burst of keystrokes produces exactly one query`() = testScope.runTest {
        // The real shape of typing: six characters in quick succession.
        val viewModel = loadedViewModel()

        listOf("R", "Ri", "Riv", "Rive", "River", "Rivera").forEach { viewModel.onSearchTextChange(it) }
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(
            "one query per burst, not one per character",
            1,
            repository.requestedQueries.size,
        )
        assertEquals("Rivera", repository.requestedQueries.single().searchText)
    }

    @Test
    fun `the debounce restarts rather than letting the first keystroke through`() = testScope.runTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchTextChange("R")
        advanceTimeBy(250)
        viewModel.onSearchTextChange("Ri")
        // Past the first keystroke's timer, short of the second's. No
        // `advanceUntilIdle()` here: it would run the still-pending delay and
        // make the assertion below unmakeable.
        advanceTimeBy(100)

        assertFalse(
            "the second keystroke must delay the request, not queue behind the first",
            repository.wasQueriedAtAll,
        )

        advanceTimeBy(200)
        advanceUntilIdle()
        assertEquals("Ri", repository.requestedQueries.single().searchText)
    }

    @Test
    fun `the request carries the text as finally typed`() = testScope.runTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchTextChange("Rivera")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals("Rivera", repository.requestedQueries.single().searchText)
    }

    @Test
    fun `a cleared search field is sent as no filter at all`() = testScope.runTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchTextChange("Rivera")
        advanceTimeBy(300)
        advanceUntilIdle()

        viewModel.onSearchTextChange("")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(
            "`takeIf { it.isNotBlank() }` means a blank search is a null, not an empty string",
            listOf("Rivera", null),
            repository.requestedQueries.map { it.searchText },
        )
    }

    @Test
    fun `a whitespace-only search field is sent as no filter at all`() = testScope.runTest {
        val viewModel = loadedViewModel()

        viewModel.onSearchTextChange("   ")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertNull(repository.requestedQueries.single().searchText)
    }

    @Test
    fun `the typed text stays in the state`() = testScope.runTest {
        val viewModel = viewModel()

        viewModel.onSearchTextChange("Rivera")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals("Rivera", viewModel.uiState.value.searchText)
    }

    // --- Filters are not debounced -------------------------------------------

    @Test
    fun `choosing a specialty queries immediately`() = testScope.runTest {
        val viewModel = loadedViewModel()

        viewModel.onSpecialtySelected(Specialty.PEDIATRICS)
        advanceUntilIdle()

        assertEquals(
            "a chip press is deliberate; waiting 300 ms would feel broken",
            1,
            repository.requestedQueries.size,
        )
        assertEquals(Specialty.PEDIATRICS, repository.requestedQueries.single().specialty)
    }

    @Test
    fun `clearing the specialty queries immediately and sends no filter`() = testScope.runTest {
        val viewModel = loadedViewModel()

        viewModel.onSpecialtySelected(Specialty.PEDIATRICS)
        advanceUntilIdle()
        viewModel.onSpecialtySelected(null)
        advanceUntilIdle()

        assertEquals(listOf(Specialty.PEDIATRICS, null), repository.requestedQueries.map { it.specialty })
    }

    @Test
    fun `the chosen specialty stays in the state`() = testScope.runTest {
        val viewModel = viewModel()

        viewModel.onSpecialtySelected(Specialty.PEDIATRICS)
        advanceUntilIdle()

        assertEquals(Specialty.PEDIATRICS, viewModel.uiState.value.selectedSpecialty)
    }

    @Test
    fun `a query carries the search and the filter together`() = testScope.runTest {
        val viewModel = viewModel()

        viewModel.onSpecialtySelected(Specialty.PEDIATRICS)
        advanceUntilIdle()
        viewModel.onSearchTextChange("Rivera")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(
            DoctorQuery(searchText = "Rivera", specialty = Specialty.PEDIATRICS),
            repository.requestedQueries.last(),
        )
    }

    // --- Results -------------------------------------------------------------

    @Test
    fun `the doctors reach the state`() = testScope.runTest {
        repository.doctors = listOf(
            TestFixtures.doctor(id = "d-1", fullName = "Rivera"),
            TestFixtures.doctor(id = "d-2", fullName = "Dela Cruz"),
        )
        val viewModel = viewModel()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            listOf("Rivera", "Dela Cruz"),
            doctorsOf(viewModel.uiState.value)?.map { it.fullName },
        )
    }

    @Test
    fun `a list is loading while the request is in flight`() = testScope.runTest {
        val inFlight = CompletableDeferred<Unit>()
        repository.getDoctorsGate = inFlight
        val viewModel = viewModel()

        viewModel.refresh()
        runCurrent()

        assertTrue(
            "the spinner is what the screen shows the moment the user asks",
            viewModel.uiState.value.doctors is LoadState.Loading,
        )

        inFlight.complete(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.doctors is LoadState.Success)
    }

    @Test
    fun `a re-query shows loading again instead of keeping the stale results`() = testScope.runTest {
        repository.doctors = listOf(TestFixtures.doctor(fullName = "Before"))
        val viewModel = viewModel()
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(listOf("Before"), doctorsOf(viewModel.uiState.value)?.map { it.fullName })

        val inFlight = CompletableDeferred<Unit>()
        repository.getDoctorsGate = inFlight
        repository.doctors = listOf(TestFixtures.doctor(fullName = "After"))
        viewModel.onSpecialtySelected(Specialty.PEDIATRICS)
        runCurrent()

        assertTrue(
            "a spinner over stale results reads as a broken filter",
            viewModel.uiState.value.doctors is LoadState.Loading,
        )
        assertNull(
            "and the stale results must be gone, not merely covered",
            doctorsOf(viewModel.uiState.value),
        )

        inFlight.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("After"), doctorsOf(viewModel.uiState.value)?.map { it.fullName })
    }

    @Test
    fun `an unconnected backend is an empty list, not an error`() = testScope.runTest {
        repository.getDoctorsError = BackendNotConnectedException()
        val viewModel = viewModel()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            emptyList<Doctor>(),
            doctorsOf(viewModel.uiState.value),
        )
    }

    @Test
    fun `a refused search is an error carrying the server's sentence`() = testScope.runTest {
        repository.getDoctorsError = ApiFailure(503, "unavailable", "The clinic directory is busy.")
        val viewModel = viewModel()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            "The clinic directory is busy.",
            (viewModel.uiState.value.doctors as? LoadState.Error)?.message,
        )
    }

    @Test
    fun `an unexpected failure falls back to a message written for a person`() = testScope.runTest {
        repository.getDoctorsError = IOException("failed to connect to /10.0.2.2:8099")
        val viewModel = viewModel()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            "Couldn't load doctors. Try again in a moment.",
            (viewModel.uiState.value.doctors as? LoadState.Error)?.message,
        )
    }

    @Test
    fun `a failed search does not keep the doctor list as a stale success`() = testScope.runTest {
        repository.doctors = listOf(TestFixtures.doctor(fullName = "Before"))
        val viewModel = viewModel()
        viewModel.refresh()
        advanceUntilIdle()

        repository.getDoctorsError = ApiFailure(500, "internal_error", "Something went wrong.")
        viewModel.onSearchTextChange("Rivera")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertTrue(
            "results that no longer match the query must not stay on screen as if they did",
            viewModel.uiState.value.doctors is LoadState.Error,
        )
    }

    @Test
    fun `a retry after a failed search clears the error`() = testScope.runTest {
        repository.getDoctorsError = ApiFailure(500, "internal_error", "Something went wrong.")
        val viewModel = viewModel()
        viewModel.refresh()
        advanceUntilIdle()
        assertTrue("precondition: an error is showing", viewModel.uiState.value.doctors is LoadState.Error)

        repository.getDoctorsError = null
        repository.doctors = listOf(TestFixtures.doctor(fullName = "Rivera"))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf("Rivera"), doctorsOf(viewModel.uiState.value)?.map { it.fullName })
    }
}
