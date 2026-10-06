package com.example.mediq.ui.feature.appointments

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.LoadState
import com.example.mediq.fake.FakeAppointmentRepository
import com.example.mediq.fake.MainDispatcherRule
import com.example.mediq.fake.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Covers loading one appointment by the id its route carries.
 *
 * The bug this pins: `AppointmentDetailsScreen` took `appointmentId` from its
 * route and never used it. The composable rendered a fixed `EmptyState` — "No
 * appointment selected" — for every appointment, so tapping a real appointment
 * in the list led to a screen that claimed none existed. The signature asked for
 * the id and the body ignored it, which is not something the compiler or any
 * existing test could see.
 *
 * The sharpest assertion here is [a null id makes no network call]: it is the
 * one that distinguishes "loaded the appointment the user tapped" from "loaded
 * something, somehow".
 */
class AppointmentDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAppointmentRepository
    private val appointmentId = "appointment-42"

    @Before
    fun setUp() {
        repository = FakeAppointmentRepository()
    }

    private fun viewModelFor(id: String?) =
        AppointmentDetailsViewModel(repository, id)

    // --- Loading -------------------------------------------------------------

    @Test
    fun `the appointment is fetched using the id from the route`() {
        repository.appointment = TestFixtures.appointment(id = appointmentId)

        val state = viewModelFor(appointmentId).uiState.value

        assertEquals(listOf(appointmentId), repository.requestedIds)
        val loaded = (state.appointment as? LoadState.Success)?.data
        assertEquals(appointmentId, loaded?.id)
    }

    @Test
    fun `a failed load surfaces the server's message`() {
        // The server answers a missing appointment with 404 "That appointment was
        // not found." — a sentence written for the patient, which is what they
        // should read rather than a generic failure.
        repository.getAppointmentError =
            ApiFailure(404, "not_found", "That appointment was not found.")

        val state = viewModelFor(appointmentId).uiState.value

        assertEquals(
            "That appointment was not found.",
            (state.appointment as? LoadState.Error)?.message,
        )
    }

    @Test
    fun `a null id explains itself without contacting the server`() {
        val state = viewModelFor(null).uiState.value

        assertFalse(
            "there is nothing to fetch, so nothing should be fetched",
            repository.wasCalledAtAll,
        )
        val error = (state.appointment as? LoadState.Error)?.message
        assertTrue("expected an explanatory message, got null", !error.isNullOrBlank())
    }

    @Test
    fun `a blank id is treated the same as a missing one`() {
        val state = viewModelFor("").uiState.value

        assertFalse(repository.wasCalledAtAll)
        assertTrue(state.appointment is LoadState.Error)
        // Pinned so the guard cannot be weakened to `id == null` alone, which
        // would send an empty id to the server.
        assertEquals(
            "No appointment was selected.",
            (state.appointment as? LoadState.Error)?.message,
        )
    }

    @Test
    fun `an unconnected backend is an error here, not an empty success`() {
        // The trap this guards. `BackendNotConnectedException` maps to
        // `LoadState.Success(emptyList())` on *list* reads, where "nothing to
        // show" and "no backend" look the same. This is a single-entity read: a
        // success carrying no appointment would be a lie, because the patient
        // demonstrably has one.
        repository.getAppointmentError = BackendNotConnectedException()

        val state = viewModelFor(appointmentId).uiState.value

        assertTrue(
            "a single-entity read must not report success when it loaded nothing",
            state.appointment is LoadState.Error,
        )
        // The message too, not just the type: asserting only `is LoadState.Error`
        // leaves this test green when the whole branch is deleted and the
        // exception falls through to the generic handler. Verified by mutation.
        assertEquals(
            "This appointment isn't available right now.",
            (state.appointment as? LoadState.Error)?.message,
        )
    }

    @Test
    fun `an unexpected failure degrades to a readable message`() {
        repository.getAppointmentError = IllegalStateException("HTTP 500")

        val state = viewModelFor(appointmentId).uiState.value

        val message = (state.appointment as? LoadState.Error)?.message
        // Never a framework message: "HTTP 500" is what a patient saw before
        // `call {}` learned to read the server's error body.
        assertFalse(message.orEmpty().contains("HTTP"))
        assertTrue(!message.isNullOrBlank())
    }

    // --- Everything the details screen has to render ------------------------

    @Test
    fun `a loaded appointment carries the fields the screen displays`() {
        repository.appointment = TestFixtures.appointment(
            id = appointmentId,
            status = com.example.mediq.domain.model.AppointmentStatus.PENDING_CONFIRMATION,
            reasonForVisit = "Persistent cough",
        )

        val loaded = (viewModelFor(appointmentId).uiState.value.appointment as LoadState.Success).data

        // These are the values the screen renders. Asserting them here means a
        // refactor that drops one fails a test rather than silently producing a
        // screen with a blank field.
        assertEquals("Dr. Rivera", loaded.doctor.displayName)
        assertEquals("Internal Medicine", loaded.doctor.specialty.displayName)
        assertEquals("Awaiting confirmation", loaded.status.displayName)
        assertEquals(70000, loaded.fee.amountInCentavos)
        assertEquals("Main Building", loaded.location.building)
        assertEquals("Persistent cough", loaded.reasonForVisit)
    }
}
