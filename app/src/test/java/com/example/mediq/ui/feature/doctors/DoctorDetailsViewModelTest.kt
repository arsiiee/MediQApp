package com.example.mediq.ui.feature.doctors

import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.SlotStatus
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

    // --- What the picker is allowed to draw ---------------------------------
    //
    // Figure 4 draws a slot another patient already claimed, greyed, under the
    // legend "Grey = already claimed by another patient". The screen used to
    // filter the list to bookable slots, so nothing else could reach the grid
    // and `MediQSlotChip`'s Claimed state had no user. These pin what is drawn
    // now, and the requirement each one comes from: the legend may describe a
    // reserved slot, so a *blocked* slot — the clinic's own leave, not another
    // patient — must not be drawn under it.

    private fun TestScope.loadedViewModel(repository: FakeDoctorRepository): DoctorDetailsViewModel {
        val viewModel = DoctorDetailsViewModel(repository, "doctor-1")
        advanceUntilIdle()
        viewModel.onDateSelected(LocalDate.of(2026, 12, 1))
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `an available slot and a claimed one are both drawn`() = testScope.runTest {
        val available = TestFixtures.timeSlot(id = "slot-1", status = SlotStatus.AVAILABLE)
        val claimed = TestFixtures.timeSlot(id = "slot-2", timeOfDay = "01:30:00", status = SlotStatus.RESERVED)
        val repository = FakeDoctorRepository().apply {
            slots = listOf(available, claimed)
        }

        val drawn = loadedViewModel(repository).uiState.value.visibleSlots

        assertTrue("the picker must still be given the bookable slot", drawn is LoadState.Success)
        assertEquals(
            "a claimed slot is drawn grey rather than dropped",
            listOf(SlotStatus.AVAILABLE, SlotStatus.RESERVED),
            (drawn as LoadState.Success).data.map { it.status },
        )
        assertEquals(
            "the statuses must reach the chip intact, or it cannot pick its state",
            listOf("slot-1", "slot-2"),
            drawn.data.map { it.id },
        )
    }

    @Test
    fun `a clinic-blocked slot is not drawn`() = testScope.runTest {
        val repository = FakeDoctorRepository().apply {
            slots = listOf(TestFixtures.timeSlot(status = SlotStatus.BLOCKED))
        }

        val drawn = loadedViewModel(repository).uiState.value.visibleSlots

        assertTrue(drawn is LoadState.Success)
        assertEquals(
            "the legend would call a blocked slot another patient's claim",
            emptyList<TimeSlot>(),
            (drawn as LoadState.Success).data,
        )
    }

    @Test
    fun `a slot whose status this build cannot read is not drawn`() = testScope.runTest {
        val repository = FakeDoctorRepository().apply {
            slots = listOf(TestFixtures.timeSlot(status = SlotStatus.UNKNOWN))
        }

        val drawn = loadedViewModel(repository).uiState.value.visibleSlots

        assertTrue(drawn is LoadState.Success)
        assertEquals(
            "an unreadable status must not be offered as either bookable or claimed",
            emptyList<TimeSlot>(),
            (drawn as LoadState.Success).data,
        )
    }

    @Test
    fun `the slots the server returned are left untouched`() = testScope.runTest {
        val repository = FakeDoctorRepository().apply {
            slots = listOf(
                TestFixtures.timeSlot(id = "slot-1", status = SlotStatus.AVAILABLE),
                TestFixtures.timeSlot(id = "slot-2", timeOfDay = "01:30:00", status = SlotStatus.RESERVED),
                TestFixtures.timeSlot(id = "slot-3", timeOfDay = "02:00:00", status = SlotStatus.BLOCKED),
            )
        }

        val viewModel = loadedViewModel(repository)

        assertEquals(
            "the filter is a view over the state, not a rewrite of what the server said",
            3,
            viewModel.uiState.value.slots.let { (it as LoadState.Success).data.size },
        )
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
