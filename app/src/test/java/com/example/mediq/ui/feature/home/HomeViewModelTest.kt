package com.example.mediq.ui.feature.home

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.LoadState
import com.example.mediq.fake.FakeAppointmentRepository
import com.example.mediq.fake.FakeDoctorRepository
import com.example.mediq.fake.MainDispatcherRule
import com.example.mediq.fake.TestFixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

/**
 * Covers the home screen's two independent loads.
 *
 * **The next appointment is the soonest one, not the first one the server
 * happened to return.** `HomeViewModel` takes `minByOrNull { it.startsAt }` over
 * the upcoming list. The server's ordering is not a contract the client should
 * depend on, and the bug this pins is quiet: a home screen offering December 30th
 * as "your next appointment" to a patient with a visit tomorrow is wrong data on
 * the first screen of the app, with no error anywhere.
 *
 * **The two loads are independent, and that is the point of them being separate
 * requests.** A slow doctor list must not hold back the next appointment, and one
 * failing must not blank the other — [a failing appointment read leaves the
 * doctors alone] and its mirror pin that.
 *
 * The `LoadState` mapping follows `AGENTS.md`: an unconnected backend is a
 * `Success(null)` for the appointment (there may genuinely be no next one) and a
 * `Success(emptyList())` for the doctors. Both are states the screen can draw,
 * neither is an error banner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var doctorRepository: FakeDoctorRepository
    private lateinit var appointmentRepository: FakeAppointmentRepository

    /**
     * Built on first use, not in `setUp`.
     *
     * `init` fires both loads, and `MainDispatcherRule`'s
     * `UnconfinedTestDispatcher` runs them eagerly to completion — a ViewModel
     * constructed in `setUp` would have already settled before the fixture was in
     * place.
     */
    private val viewModel: HomeViewModel by lazy {
        HomeViewModel(doctorRepository, appointmentRepository)
    }

    @Before
    fun setUp() {
        doctorRepository = FakeDoctorRepository()
        appointmentRepository = FakeAppointmentRepository()
    }

    private fun appointment(id: String, startsAt: String) = TestFixtures.appointment(
        id = id,
        startsAt = Instant.parse(startsAt),
    )

    private fun nextAppointment() =
        ((viewModel.uiState.value.nextAppointment as? LoadState.Success)?.data as? Appointment)?.id

    private fun doctorNames() =
        ((viewModel.uiState.value.doctorsWithOpenSlots as? LoadState.Success)?.data)?.map { it.fullName }

    // --- The next appointment is the soonest ----------------------------------

    @Test
    fun `the next appointment is the soonest one, not the first returned`() {
        // Deliberately in descending order, with the soonest last. `first()` and
        // `maxByOrNull` both pass a test written in server order; only this
        // ordering tells them apart.
        appointmentRepository.appointments = listOf(
            appointment("far", "2026-12-30T01:00:00Z"),
            appointment("soonest", "2026-11-02T01:00:00Z"),
            appointment("middle", "2026-11-20T01:00:00Z"),
        )

        assertEquals("soonest", nextAppointment())
    }

    @Test
    fun `upcoming appointments are requested, not history`() {
        appointmentRepository.appointments = listOf(appointment("a", "2026-11-02T01:00:00Z"))

        viewModel.uiState.value

        assertEquals(listOf(AppointmentFilter.UPCOMING), appointmentRepository.requestedFilters)
    }

    @Test
    fun `no upcoming appointment is a successful null, not an error`() {
        appointmentRepository.appointments = emptyList()

        assertEquals(LoadState.Success(null), viewModel.uiState.value.nextAppointment)
    }

    // --- Two independent loads ------------------------------------------------

    @Test
    fun `both loads happen without waiting for each other`() {
        appointmentRepository.appointments = listOf(appointment("a", "2026-11-02T01:00:00Z"))
        doctorRepository.doctors = listOf(TestFixtures.doctor(fullName = "Rivera"))

        viewModel.uiState.value

        assertEquals(listOf(AppointmentFilter.UPCOMING), appointmentRepository.requestedFilters)
        assertTrue(
            "a slow doctor list must not hold back the appointment",
            doctorRepository.wasQueriedAtAll,
        )
    }

    @Test
    fun `a failing appointment read leaves the doctors alone`() {
        appointmentRepository.getAppointmentsError =
            ApiFailure(500, "internal_error", "Something went wrong.")
        doctorRepository.doctors = listOf(
            TestFixtures.doctor(id = "d-1", fullName = "Rivera"),
            TestFixtures.doctor(id = "d-2", fullName = "Dela Cruz"),
        )

        val state = viewModel.uiState.value

        assertEquals(
            "Something went wrong.",
            (state.nextAppointment as? LoadState.Error)?.message,
        )
        assertEquals(listOf("Rivera", "Dela Cruz"), doctorNames())
    }

    @Test
    fun `a failing doctor read leaves the next appointment alone`() {
        appointmentRepository.appointments = listOf(appointment("soonest", "2026-11-02T01:00:00Z"))
        doctorRepository.getDoctorsError =
            ApiFailure(500, "internal_error", "Couldn't load doctors. Check your connection and try again.")

        val state = viewModel.uiState.value

        assertEquals("soonest", nextAppointment())
        assertTrue(state.doctorsWithOpenSlots is LoadState.Error)
    }

    @Test
    fun `an unreachable backend is a null appointment and an empty doctor list`() {
        appointmentRepository.getAppointmentsError = BackendNotConnectedException()
        doctorRepository.getDoctorsError = BackendNotConnectedException()

        val state = viewModel.uiState.value

        assertEquals(LoadState.Success(null), state.nextAppointment)
        assertEquals(emptyList<Doctor>(), doctorNames())
    }

    @Test
    fun `an unexpected appointment failure falls back to a message written for a person`() {
        appointmentRepository.getAppointmentsError = IOException("failed to connect to /10.0.2.2:8099")

        val state = viewModel.uiState.value

        assertEquals(
            "Couldn't load your appointments. Try again in a moment.",
            (state.nextAppointment as? LoadState.Error)?.message,
        )
    }

    @Test
    fun `an unexpected doctor failure falls back to a message written for a person`() {
        doctorRepository.getDoctorsError = IOException("failed to connect to /10.0.2.2:8099")

        val state = viewModel.uiState.value

        assertEquals(
            "Couldn't load doctors. Check your connection and try again.",
            (state.doctorsWithOpenSlots as? LoadState.Error)?.message,
        )
    }

    // --- Refreshing -----------------------------------------------------------

    @Test
    fun `a refresh re-reads both halves`() {
        appointmentRepository.appointments = listOf(appointment("a", "2026-11-02T01:00:00Z"))
        doctorRepository.doctors = listOf(TestFixtures.doctor(fullName = "Before"))
        viewModel.uiState.value

        appointmentRepository.appointments = listOf(appointment("b", "2026-11-03T01:00:00Z"))
        doctorRepository.doctors = listOf(TestFixtures.doctor(fullName = "After"))
        viewModel.refresh()

        assertEquals("b", nextAppointment())
        assertEquals(listOf("After"), doctorNames())
        assertEquals(
            listOf(AppointmentFilter.UPCOMING, AppointmentFilter.UPCOMING),
            appointmentRepository.requestedFilters,
        )
    }

    @Test
    fun `a refresh recovers from a previous error`() {
        doctorRepository.getDoctorsError = ApiFailure(500, "internal_error", "Something went wrong.")
        assertTrue(
            "precondition: an error is showing",
            viewModel.uiState.value.doctorsWithOpenSlots is LoadState.Error,
        )

        doctorRepository.getDoctorsError = null
        doctorRepository.doctors = listOf(TestFixtures.doctor(fullName = "Rivera"))
        viewModel.refresh()

        assertNotNull(doctorNames())
        assertTrue(viewModel.uiState.value.doctorsWithOpenSlots is LoadState.Success)
    }

    @Test
    fun `a later appointment arriving earlier is picked up on refresh`() {
        // The reason `refresh()` matters here rather than being a convenience:
        // the soonest-appointment rule is only as good as the last read.
        appointmentRepository.appointments = listOf(appointment("far", "2026-12-30T01:00:00Z"))
        assertEquals("far", nextAppointment())

        appointmentRepository.appointments = listOf(
            appointment("far", "2026-12-30T01:00:00Z"),
            appointment("urgent", "2026-11-01T01:00:00Z"),
        )
        viewModel.refresh()

        assertEquals("urgent", nextAppointment())
    }

    // --- Concurrent updates to the two halves --------------------------------

    @Test
    fun `a doctor read that lands after the appointment read does not erase it`() {
        // The bug this pins: both `init` loads do
        // `_uiState.value = _uiState.value.copy(...)`, which is a *read* followed
        // by a suspend and then a *write*. Two coroutines can both read the state
        // before either writes, so the second write is built from a stale copy and
        // resurrects `Loading` for the field the first one already filled.
        //
        // This needs a real dispatcher to happen, and the existing suite cannot see
        // it: `MainDispatcherRule` runs an `UnconfinedTestDispatcher`, where each
        // launch runs to completion before the next begins, so the two reads can
        // never interleave. Hence this one test drives its own
        // `StandardTestDispatcher` and completes the two gates in an order that
        // puts both coroutines in flight at once.
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val appointmentGate = CompletableDeferred<Unit>()
            val doctorGate = CompletableDeferred<Unit>()
            appointmentRepository.getAppointmentsGate = appointmentGate
            doctorRepository.getDoctorsGate = doctorGate
            appointmentRepository.appointments =
                listOf(appointment("soonest", "2026-11-02T01:00:00Z"))
            doctorRepository.doctors = listOf(TestFixtures.doctor(fullName = "Rivera"))

            val model = HomeViewModel(doctorRepository, appointmentRepository)
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(
                "precondition: both loads are in flight",
                listOf(LoadState.Loading, LoadState.Loading),
                listOf(model.uiState.value.nextAppointment, model.uiState.value.doctorsWithOpenSlots),
            )

            // Both loads have now read the state and are suspended inside their
            // repository call. Releasing them here is the interleaving that loses
            // an update when the write is a plain assignment.
            appointmentGate.complete(Unit)
            doctorGate.complete(Unit)
            dispatcher.scheduler.advanceUntilIdle()

            val state = model.uiState.value
            assertTrue(
                "the appointment half was erased by the doctor half's write: ${state.nextAppointment}",
                state.nextAppointment is LoadState.Success,
            )
            assertTrue(
                "the doctor half was erased by the appointment half's write: ${state.doctorsWithOpenSlots}",
                state.doctorsWithOpenSlots is LoadState.Success,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `a doctor query carries no filter on the home screen`() {
        doctorRepository.doctors = listOf(TestFixtures.doctor())

        viewModel.uiState.value

        assertNull(
            "the home screen shows everyone, so nothing may narrow the query",
            doctorRepository.requestedQueries.single().specialty,
        )
    }
}
