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
}
