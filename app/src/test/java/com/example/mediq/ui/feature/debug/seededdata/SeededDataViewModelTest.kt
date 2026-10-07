package com.example.mediq.ui.feature.debug.seededdata

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Specialty
import com.example.mediq.fake.FakeDoctorRepository
import com.example.mediq.fake.MainDispatcherRule
import com.example.mediq.fake.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Covers the read behind the temporary seeded-data screen.
 *
 * The screen exists to answer one question — *is the backend actually working?* —
 * and a screen that answers it wrongly is worse than no screen. So the claims
 * guarded here are not "the list renders" but "the list is the network's answer and
 * nothing else". That makes [the state holds exactly what the repository returned]
 * the load-bearing case in this file: a fallback list behind the repository call
 * would render three invented doctors with the backend dead, and the screen would
 * report a healthy clinic while proving nothing. It would also break the rule in
 * `AGENTS.md` that fabricated doctors must not exist in `app/` — which is why
 * `DemoData.kt:19` labels them on the server side instead.
 *
 * The `BackendNotConnectedException` branch follows the documented rule in
 * `AGENTS.md`: on a **list** read an unreachable backend is `Success(emptyList())`,
 * not an error. Note what that rule costs this screen, and why [an empty list is a
 * success, not an error] and [an unconnected backend is an empty list, not an
 * error] both land on the same state — `RetrofitDoctorRepository` absorbs an
 * unreachable server and returns an empty `Paged`, so by the time this ViewModel
 * sees anything, "the server is down" and "the seed never ran" are genuinely the
 * same observation. The screen tells them apart by *message*, in `ui/` alone.
 */
class SeededDataViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeDoctorRepository

    /**
     * Built on first use, not in `setUp`.
     *
     * This ViewModel loads in `init`, and `MainDispatcherRule` installs an
     * `UnconfinedTestDispatcher` that runs that launch eagerly to completion.
     * Constructing it in `setUp` would finish the read *before* a test sets
     * `repository.doctors`, and every assertion would be reading a settled empty
     * list instead of the fixture — a green suite that asserts nothing. `lazy`
     * defers construction to the first read, which is after the fixture is in
     * place. Same trap as `NotificationsViewModelTest`.
     */
    private val viewModel: SeededDataViewModel by lazy { SeededDataViewModel(repository) }

    @Before
    fun setUp() {
        repository = FakeDoctorRepository()
    }

    // --- Success -------------------------------------------------------------

    @Test
    fun `the doctors the server returned reach the state`() {
        val seeded = listOf(
            TestFixtures.doctor(id = "doctor-1", fullName = "Rivera"),
            TestFixtures.doctor(id = "doctor-2", fullName = "Santos", specialty = Specialty.PEDIATRICS),
            TestFixtures.doctor(id = "doctor-3", fullName = "Cruz", specialty = Specialty.DERMATOLOGY),
        )
        repository.doctors = seeded

        val doctors = (viewModel.uiState.value.doctors as? LoadState.Success)?.data

        assertEquals(seeded, doctors)
    }

    @Test
    fun `the read is unfiltered`() {
        viewModel.uiState.value

        // No search text and no specialty: this screen shows what the seed
        // inserted, not what a filter would select. A query carrying a filter
        // could return fewer rows than the seed created and read as a partial
        // seed, which is a different problem from a dead backend.
        assertEquals(DoctorQuery(), repository.requestedQueries.single())
    }

    // --- The three failure modes --------------------------------------------

    @Test
    fun `an empty list is a success, not an error`() {
        repository.doctors = emptyList()

        val state = viewModel.uiState.value.doctors

        assertEquals(LoadState.Success(emptyList<Doctor>()), state)
    }

    @Test
    fun `an unconnected backend is an empty list, not an error`() {
        repository.getDoctorsError = BackendNotConnectedException()

        val state = viewModel.uiState.value.doctors

        assertEquals(LoadState.Success(emptyList<Doctor>()), state)
    }

    @Test
    fun `a server failure shows the server's own sentence`() {
        val serverSentence = "The clinic system is unavailable right now. Please try again shortly."
        repository.getDoctorsError = ApiFailure(500, "internal_error", serverSentence)

        val state = viewModel.uiState.value.doctors

        assertEquals(LoadState.Error(serverSentence), state)
    }

    /**
     * The `ApiFailure` branch must surface [ApiFailure.message] and nothing else.
     *
     * This is the bug the error contract exists for. Retrofit throws
     * `HttpException`, whose `message` is `"HTTP 500 "` — framework text the
     * server never wrote — and a screen that reports "the backend is working"
     * while showing a patient their own HTTP status is worse than no screen. The
     * negative assertion is the load-bearing half: `assertEquals` alone would also
     * pass if the framework's text happened to match.
     */
    @Test
    fun `a failure message is the server's, never a framework message`() {
        repository.getDoctorsError = ApiFailure(500, "internal_error", "Couldn't load doctors.")

        val message = (viewModel.uiState.value.doctors as? LoadState.Error)?.message

        assertEquals("Couldn't load doctors.", message)
        assertFalse(message.orEmpty().contains("HTTP"))
    }

    // --- The claim this screen rests on --------------------------------------

    /**
     * No fallback list. The state is the repository's answer, verbatim.
     *
     * Written against the state rather than by inspecting the source, because the
     * failure it guards is not a literal someone typed — it is a fallback *path*,
     * e.g. `items.ifEmpty { listOf(demoDoctor) }`, which reads as reasonable and
     * would make the screen report a healthy backend with the server down. Three
     * scenarios rather than one: a count is what a fallback corrupts, so each
     * answer is checked for its own size, and the third asserts the state tracks
     * a *shrinking* answer rather than only ever growing.
     */
    @Test
    fun `the state holds exactly what the repository returned`() {
        repository.doctors = listOf(
            TestFixtures.doctor(id = "doctor-1"),
            TestFixtures.doctor(id = "doctor-2"),
            TestFixtures.doctor(id = "doctor-3"),
        )
        val three = viewModel.uiState.value.doctors

        repository.doctors = listOf(TestFixtures.doctor(id = "doctor-9"))
        viewModel.refresh()
        val one = viewModel.uiState.value.doctors

        repository.doctors = emptyList()
        viewModel.refresh()
        val none = viewModel.uiState.value.doctors

        assertEquals(3, (three as LoadState.Success).data.size)
        assertEquals(1, (one as LoadState.Success).data.size)
        assertEquals(emptyList<Doctor>(), (none as LoadState.Success).data)
    }

    // --- Reload --------------------------------------------------------------

    @Test
    fun `refresh re-queries the server and replaces the rows`() {
        repository.doctors = listOf(TestFixtures.doctor(id = "doctor-1"))
        viewModel.uiState.value
        assertTrue(repository.wasQueriedAtAll)

        repository.doctors = listOf(
            TestFixtures.doctor(id = "doctor-1"),
            TestFixtures.doctor(id = "doctor-2"),
        )
        viewModel.refresh()

        // Two reads: the `init` load and the refresh. Without this the test would
        // pass against a ViewModel that cached its first answer — which is the
        // failure mode of a screen whose whole job is reporting current truth.
        assertEquals(2, repository.requestedQueries.size)
        assertEquals(2, (viewModel.uiState.value.doctors as LoadState.Success).data.size)
    }
}
