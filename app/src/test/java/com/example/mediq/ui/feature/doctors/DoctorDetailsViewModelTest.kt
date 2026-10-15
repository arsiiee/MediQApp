package com.example.mediq.ui.feature.doctors

import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.repository.DoctorRepository
import com.example.mediq.fake.FakeDoctorRepository
import com.example.mediq.fake.TestFixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
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
import java.time.LocalDate

/**
 * Pins the ordering race that kept the details screen on a spinner: [DoctorDetailsViewModel]
 * launches `loadDoctor` and `loadAvailableDates` together, and a late writer used to
 * clobber the other field back to `Loading`. With gated repository calls, whichever
 * response wins must not touch the other field's outcome.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DoctorDetailsViewModelTest {

    private val assertScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(assertScheduler)
    private val testScope = TestScope(assertScheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the late doctor response cannot wipe out the available dates`() = testScope.runTest {
        val datesGate = CompletableDeferred<Unit>()
        val doctorGate = CompletableDeferred<Unit>()
        val repository = object : DoctorRepository {
            override suspend fun getDoctors(query: DoctorQuery): Paged<Doctor> = Paged(emptyList())
            override suspend fun getDoctor(doctorId: String): Doctor {
                doctorGate.await()
                return TestFixtures.doctor()
            }
            override suspend fun getAvailableDates(doctorId: String, month: LocalDate): List<AvailableDate> {
                datesGate.await()
                return listOf(TestFixtures.availableDate())
            }
            override suspend fun getSlots(doctorId: String, date: LocalDate): List<TimeSlot> = emptyList()
        }

        val viewModel = DoctorDetailsViewModel(repository, "doctor-1")
        runCurrent()

        // Dates win the race.
        datesGate.complete(Unit)
        runCurrent()

        // The doctor arrives second and finishes — it must not stamp `Loading`
        // back over the dates that already landed.
        doctorGate.complete(Unit)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.doctor is LoadState.Success)
        assertTrue(
            "availableDates reverted to ${state.availableDates}",
            state.availableDates is LoadState.Success,
        )
    }

    @Test
    fun `the late dates response cannot wipe out the doctor`() = testScope.runTest {
        val datesGate = CompletableDeferred<Unit>()
        val doctorGate = CompletableDeferred<Unit>()
        val repository = object : DoctorRepository {
            override suspend fun getDoctors(query: DoctorQuery): Paged<Doctor> = Paged(emptyList())
            override suspend fun getDoctor(doctorId: String): Doctor {
                doctorGate.await()
                return TestFixtures.doctor()
            }
            override suspend fun getAvailableDates(doctorId: String, month: LocalDate): List<AvailableDate> {
                datesGate.await()
                return listOf(TestFixtures.availableDate())
            }
            override suspend fun getSlots(doctorId: String, date: LocalDate): List<TimeSlot> = emptyList()
        }

        val viewModel = DoctorDetailsViewModel(repository, "doctor-1")
        runCurrent()

        doctorGate.complete(Unit)
        runCurrent()

        datesGate.complete(Unit)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.availableDates is LoadState.Success)
        assertTrue(
            "doctor reverted to ${state.doctor}",
            state.doctor is LoadState.Success,
        )
    }

    @Test
    fun `both reads settle on success`() = testScope.runTest {
        val repository = FakeDoctorRepository().apply {
            availableDates = listOf(TestFixtures.availableDate())
        }
        val viewModel = DoctorDetailsViewModel(repository, "doctor-1")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.doctor is LoadState.Success)
        assertTrue(viewModel.uiState.value.availableDates is LoadState.Success)
    }

    // --- The month stepper ---------------------------------------------------
    //
    // The ViewModel used to ask for `YearMonth.now()` and nothing else, so a
    // doctor's next month was unreachable and a month that had rolled over could
    // not be left (SPEC-ui-redesign item 6). These pin the replacement.
    //
    // No test here injects a clock. Every assertion is expressed relative to the
    // state's own `month` / `currentMonth`, so it holds on any day the suite runs
    // and cannot rot into "the machine happened to be in November".

    @Test
    fun `the first read asks about the month the picker shows`() = testScope.runTest {
        val repository = FakeDoctorRepository()
        val viewModel = DoctorDetailsViewModel(repository, "doctor-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(state.month.atDay(1), repository.requestedMonths.first())
        assertEquals(state.month, state.currentMonth)
    }

    @Test
    fun `stepping forward re-reads availability for the month now on screen`() = testScope.runTest {
        val repository = FakeDoctorRepository().apply {
            availableDates = listOf(TestFixtures.availableDate())
        }
        val viewModel = DoctorDetailsViewModel(repository, "doctor-1")
        advanceUntilIdle()

        val thisMonth = viewModel.uiState.value.month
        viewModel.onNextMonth()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(thisMonth.plusMonths(1), state.month)
        assertEquals(
            "the stepper must ask about the month it now shows, not the old one",
            state.month.atDay(1),
            repository.requestedMonths.last(),
        )
        assertEquals(2, repository.requestedMonths.size)
    }

    @Test
    fun `the stepper refuses to page into a month already gone`() = testScope.runTest {
        val repository = FakeDoctorRepository()
        val viewModel = DoctorDetailsViewModel(repository, "doctor-1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canGoToPreviousMonth)

        viewModel.onPreviousMonth()
        advanceUntilIdle()

        assertEquals(viewModel.uiState.value.currentMonth, viewModel.uiState.value.month)
        assertEquals(
            "a refused step must not reach the server",
            1,
            repository.requestedMonths.size,
        )
    }

    @Test
    fun `stepping back is allowed once the picker has left this month`() = testScope.runTest {
        val repository = FakeDoctorRepository()
        val viewModel = DoctorDetailsViewModel(repository, "doctor-1")
        advanceUntilIdle()

        viewModel.onNextMonth()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canGoToPreviousMonth)

        viewModel.onPreviousMonth()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(state.currentMonth, state.month)
        assertFalse(state.canGoToPreviousMonth)
    }

    @Test
    fun `changing the month discards a date and slot from the old one`() = testScope.runTest {
        val repository = FakeDoctorRepository().apply {
            availableDates = listOf(TestFixtures.availableDate())
            slots = listOf(TestFixtures.timeSlot())
        }
        val viewModel = DoctorDetailsViewModel(repository, "doctor-1")
        advanceUntilIdle()

        viewModel.onDateSelected(LocalDate.of(2026, 11, 10))
        advanceUntilIdle()
        viewModel.onSlotSelected(TestFixtures.timeSlot())
        assertTrue("nothing was selected to discard", viewModel.uiState.value.canContinue)

        viewModel.onNextMonth()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("a date from the old month must not stay selected", state.selectedDate)
        assertNull("a slot from the old month must not stay chosen", state.selectedSlot)
        assertFalse(state.canContinue)
    }
}
