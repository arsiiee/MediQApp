package com.example.mediq.ui.feature.appointments

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.AppointmentStatus
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.LoadState
import com.example.mediq.domain.model.RescheduleRequest
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

    // --- Cancelling ----------------------------------------------------------
    //
    // The gap this closes: `AppointmentRepository.cancel` existed and the screen
    // rendered no way to reach it, so a patient who needed to change a booking had
    // none. `AppointmentStatus.isActionable` already encodes which statuses permit
    // a change; these tests pin that the UI obeys it rather than trusting it.

    @Test
    fun `cancelling asks the server to cancel this appointment`() {
        repository.appointment = TestFixtures.appointment(
            id = appointmentId,
            status = AppointmentStatus.CONFIRMED,
        )

        viewModelFor(appointmentId).cancel()

        assertEquals(listOf(appointmentId), repository.cancelledIds)
    }

    @Test
    fun `a successful cancel reloads so the status badge is not stale`() {
        // The server frees the slot by deleting the `slot_claims` row and keeps
        // the appointment for history, so the status only changes on the server.
        // Without a reload the screen would keep saying "Confirmed" after the
        // patient cancelled.
        repository.appointment = TestFixtures.appointment(
            id = appointmentId,
            status = AppointmentStatus.CONFIRMED,
        )
        val viewModel = viewModelFor(appointmentId)

        viewModel.cancel()

        // One read from init, one from the post-cancel refresh.
        assertEquals(2, repository.requestedIds.size)
        assertEquals(appointmentId, repository.requestedIds.last())
    }

    @Test
    fun `a refused cancel shows the server's message and leaves the appointment alone`() {
        // A race the client cannot prevent: the status read says actionable, and
        // by the time the tap lands the appointment is gone. `DELETE
        // /appointments/{id}` then answers 400 with a sentence written for the
        // patient. The status stays CONFIRMED here so the tap is allowed through
        // — the refusal is the server's, which is the case worth handling.
        repository.appointment = TestFixtures.appointment(
            id = appointmentId,
            status = AppointmentStatus.CONFIRMED,
        )
        repository.cancelError = ApiFailure(400, "already_cancelled", "That appointment is already cancelled.")
        val viewModel = viewModelFor(appointmentId)
        val readsBefore = repository.requestedIds.size

        viewModel.cancel()

        assertEquals("That appointment is already cancelled.", viewModel.uiState.value.actionError)
        // No refresh: the server refused, so the appointment on screen is still
        // the truth and re-reading it would only churn.
        assertEquals(readsBefore, repository.requestedIds.size)
    }

    @Test
    fun `cancel and reschedule are not offered on a status that is not actionable`() {
        // Pinned per status rather than once for the group, because the four
        // differ in why: COMPLETED and DECLINED are settled by the clinic,
        // CANCELLED is what the patient did, and UNKNOWN must not offer a
        // mutation on the strength of a guess.
        listOf(
            AppointmentStatus.COMPLETED,
            AppointmentStatus.CANCELLED,
            AppointmentStatus.DECLINED,
            AppointmentStatus.UNKNOWN,
        ).forEach { status ->
            repository.appointment =
                TestFixtures.appointment(id = appointmentId, status = status)
            val viewModel = viewModelFor(appointmentId)

            assertFalse(
                "$status must not offer a change",
                viewModel.uiState.value.canCancelOrReschedule,
            )
        }
    }

    @Test
    fun `cancel and reschedule are offered while the appointment is still actionable`() {
        listOf(
            AppointmentStatus.PENDING_CONFIRMATION,
            AppointmentStatus.CONFIRMED,
        ).forEach { status ->
            repository.appointment =
                TestFixtures.appointment(id = appointmentId, status = status)

            assertTrue(
                "$status should offer a change",
                viewModelFor(appointmentId).uiState.value.canCancelOrReschedule,
            )
        }
    }

    // --- Rescheduling -------------------------------------------------------

    @Test
    fun `a reschedule is sent with the real appointment id and the chosen slot`() {
        repository.appointment = TestFixtures.appointment(id = appointmentId)
        val viewModel = viewModelFor(appointmentId)

        viewModel.requestReschedule("slot-9")

        assertEquals(
            listOf(RescheduleRequest(appointmentId, "slot-9")),
            repository.rescheduleRequests,
        )
    }

    @Test
    fun `rescheduling without a chosen slot never reaches the server`() {
        // The server would refuse it, but the patient would see a 4xx for a tap
        // on a button that was never meaningfully enabled. Guarded on the client
        // so the message is ours and the request is not made.
        val viewModel = viewModelFor(appointmentId)

        viewModel.requestReschedule(null)

        assertFalse(
            "no slot was chosen, so nothing should have been submitted",
            repository.wasMutated,
        )
        assertTrue(!viewModel.uiState.value.actionError.isNullOrBlank())
    }

    @Test
    fun `a refused reschedule shows the server's message`() {
        repository.requestRescheduleError =
            ApiFailure(409, "slot_taken", "That time was just taken. Please pick another.")
        val viewModel = viewModelFor(appointmentId)

        viewModel.requestReschedule("slot-9")

        assertEquals(
            "That time was just taken. Please pick another.",
            viewModel.uiState.value.actionError,
        )
    }

    @Test
    fun `an unreachable backend says the change was not made`() {
        // The dangerous failure mode for a mutation: a generic "couldn't reach"
        // with no statement about whether the cancel landed. The patient has to
        // know they still hold the booking.
        repository.cancelError = BackendNotConnectedException()
        val viewModel = viewModelFor(appointmentId)

        viewModel.cancel()

        val message = viewModel.uiState.value.actionError
        assertTrue("expected an explanation, got null", !message.isNullOrBlank())
        assertTrue(
            "the message must say the change did not happen, got: $message",
            message.orEmpty().contains("not", ignoreCase = true),
        )
    }

    @Test
    fun `starting a reschedule does not cancel anything`() {
        // A cancel and a reschedule are different requests to different endpoints.
        // A bug that fired both from one tap would show up here and nowhere else.
        val viewModel = viewModelFor(appointmentId)

        viewModel.requestReschedule("slot-9")

        assertTrue(repository.cancelledIds.isEmpty())
    }
}
