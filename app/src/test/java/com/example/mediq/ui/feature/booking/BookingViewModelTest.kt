package com.example.mediq.ui.feature.booking

import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.BackendNotConnectedException
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.fake.FakeAppointmentRepository
import com.example.mediq.fake.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant

/**
 * Covers the booking submit.
 *
 * ## `BookingSelection` is a global singleton, and that is why this file is careful
 *
 * `BookingSelection.selection` is a mutable global that survives between tests in
 * the same JVM. A test that sets a selection and does not clear it makes the
 * *next* test's "no selection" assertions fail — an order dependency that reads
 * as a production bug and is not one. [setUp] and [tearDown] both clear it, and
 * [a submission with no slot chosen never reaches the repository] depends on
 * that.
 *
 * The design reason for the global is in `BookingSelection`'s own KDoc: the slot
 * is a real object, not a route string. That is a fair trade, but it is the kind
 * of thing that is invisible until two tests start fighting, so it is stated here.
 *
 * ## What is worth pinning
 *
 * **A booking cannot be built without a real slot.** The request is assembled from
 * the global rather than from screen arguments, so [a submission with no slot
 * chosen never reaches the repository] is the guard against a request for a
 * doctor and a time that do not correspond to a real slot.
 *
 * **The 409 must show the server's sentence.** `slot_taken` says "That time was
 * just taken. Please pick another." A generic string told the patient to retry a
 * booking that can never succeed, because the slot is gone. That is the exact bug
 * `AGENTS.md` records from before the error contract was mapped.
 *
 * **The selection is cleared only on success.** Clearing it on failure would
 * throw away the patient's pick after a network blip and make them re-choose it.
 */
class BookingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAppointmentRepository
    private lateinit var viewModel: BookingViewModel

    @Before
    fun setUp() {
        repository = FakeAppointmentRepository()
        viewModel = BookingViewModel(repository)
        BookingSelection.clear()
    }

    /**
     * Cleared again afterwards, not just before.
     *
     * `BookingSelection` is a JVM-wide global, so a selection left behind by this
     * test would fail an unrelated test that runs later in the same worker — and
     * the failure would name that other test, pointing nowhere near the cause.
     */
    @After
    fun tearDown() = BookingSelection.clear()

    private fun selectSlot(id: String = "slot-9", doctorId: String = "doctor-1") {
        BookingSelection.set(
            BookingSelection.Selection(
                doctorId = doctorId,
                slotId = id,
                doctorDisplayName = "Dr. Rivera",
                specialtyDisplayName = "Internal Medicine",
                startsAt = Instant.parse("2026-12-01T01:00:00Z"),
                locationDisplay = "Main Building, 2F, Clinic 204",
                feeCentavos = 70000,
            ),
        )
    }

    /** Fills the form the way `BookingScreen` does: a reason, a confirmed tick, a slot. */
    private fun fillCompleteBooking(reason: String = "Fever and cough") {
        selectSlot()
        viewModel.onReasonChanged(reason)
        viewModel.onConfirmedChanged(true)
    }

    // --- A request cannot be built without a slot ----------------------------

    @Test
    fun `a submission with no slot chosen never reaches the repository`() {
        viewModel.onReasonChanged("Fever and cough")
        viewModel.onConfirmedChanged(true)

        viewModel.submit()

        assertEquals(
            "the request must not exist without a real slot behind it",
            "Choose a time slot first.",
            viewModel.uiState.value.error,
        )
        assertTrue(repository.bookingRequests.isEmpty())
    }

    @Test
    fun `a submission with no slot chosen does not report a booking`() {
        viewModel.onConfirmedChanged(true)

        viewModel.submit()

        assertFalse(
            "nothing was booked, so nothing may claim to have been",
            viewModel.uiState.value.booked,
        )
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    // --- Reaching the repository ---------------------------------------------

    @Test
    fun `a complete submission reaches the repository with the chosen slot`() {
        fillCompleteBooking()

        viewModel.submit()

        assertEquals("slot-9", repository.bookingRequests.single().slotId)
    }

    @Test
    fun `the reason for visit is sent as typed`() {
        fillCompleteBooking("Follow-up checkup")

        viewModel.submit()

        assertEquals("Follow-up checkup", repository.bookingRequests.single().reasonForVisit)
    }

    @Test
    fun `a blank reason is sent as no reason at all`() {
        // `takeIf { it.isNotBlank() }`, so a blank box does not become a stored
        // empty string the clinic then has to explain to a patient.
        fillCompleteBooking(reason = "   ")

        viewModel.submit()

        assertNull(repository.bookingRequests.single().reasonForVisit)
    }

    @Test
    fun `the booking is confirmed on the patient's behalf`() {
        fillCompleteBooking()

        viewModel.submit()

        assertTrue(
            "`AGENTS.md` notes the server validates this and then discards it, " +
                "hardcoding true — but the client must still send what it means",
            repository.bookingRequests.single().confirmedByPatient,
        )
    }

    // --- Success --------------------------------------------------------------

    @Test
    fun `a successful booking reports itself booked and is not submitting`() {
        fillCompleteBooking()

        viewModel.submit()

        val state = viewModel.uiState.value
        assertTrue("expected the booking to be confirmed", state.booked)
        assertFalse("a finished request is not still submitting", state.isSubmitting)
        assertNull("success is not an error", state.error)
    }

    @Test
    fun `a successful booking clears the selection`() {
        // So a stale pick cannot be re-submitted if the user navigates back and
        // enters the booking flow again — which would silently create a second
        // appointment for the same slot.
        fillCompleteBooking()

        viewModel.submit()

        assertNull("the selection outlived the booking", BookingSelection.selection)
    }

    // --- Failure --------------------------------------------------------------

    @Test
    fun `a taken slot shows the server's sentence, not a retry instruction`() {
        // The 409 that matters. "Please try again" here tells a patient to retry a
        // booking that can never succeed, because the slot is gone.
        fillCompleteBooking()
        repository.bookError =
            ApiFailure(409, "slot_taken", "That time was just taken. Please pick another.")

        viewModel.submit()

        val error = viewModel.uiState.value.error
        assertEquals("That time was just taken. Please pick another.", error)
        assertFalse("nothing was booked, so nothing may claim to have been", viewModel.uiState.value.booked)
    }

    @Test
    fun `an unconnected backend says no appointment was created`() {
        fillCompleteBooking()
        repository.bookError = BackendNotConnectedException()

        viewModel.submit()

        assertEquals(
            "the patient must be able to trust that nothing was booked",
            "Bookings aren't available yet. No appointment was created.",
            viewModel.uiState.value.error,
        )
        assertFalse(viewModel.uiState.value.booked)
    }

    @Test
    fun `an unexpected failure falls back to a message written for a person`() {
        fillCompleteBooking()
        repository.bookError = IOException("failed to connect to /10.0.2.2:8099")

        viewModel.submit()

        val error = viewModel.uiState.value.error
        assertEquals("Couldn't complete the booking. Please try again.", error)
        assertFalse(
            "the framework's message reached the user",
            error?.contains("10.0.2.2") ?: false,
        )
    }

    @Test
    fun `a failed booking stops submitting`() {
        fillCompleteBooking()
        repository.bookError = ApiFailure(409, "slot_taken", "That time was just taken.")

        viewModel.submit()

        assertFalse(
            "a stuck spinner would block the screen forever",
            viewModel.uiState.value.isSubmitting,
        )
    }

    @Test
    fun `a failed booking keeps the selection so it can be retried`() {
        // Clearing on failure would throw away the patient's pick after a network
        // blip and make them choose the same slot again by hand.
        fillCompleteBooking()
        repository.bookError = IOException("failed to connect")

        viewModel.submit()

        assertNotNull("the pick must survive a failed attempt", BookingSelection.selection)
    }

    @Test
    fun `a retry after a failure can succeed`() {
        fillCompleteBooking()
        repository.bookError = IOException("failed to connect")
        viewModel.submit()
        assertNotNull("precondition: the attempt failed", viewModel.uiState.value.error)

        repository.bookError = null
        viewModel.submit()

        assertTrue(viewModel.uiState.value.booked)
        assertNull("the stale failure must not survive a successful retry", viewModel.uiState.value.error)
        assertEquals("both attempts reached the server", 2, repository.bookingRequests.size)
    }

    // --- canSubmit ------------------------------------------------------------

    @Test
    fun `a confirmed booking with a slot chosen can be submitted`() {
        fillCompleteBooking()

        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `an unconfirmed booking cannot be submitted`() {
        selectSlot()
        viewModel.onReasonChanged("Fever and cough")

        assertFalse(
            "the patient must confirm before a slot is held for them",
            viewModel.uiState.value.canSubmit,
        )
    }

    @Test
    fun `a booking with no slot chosen cannot be submitted`() {
        viewModel.onReasonChanged("Fever and cough")
        viewModel.onConfirmedChanged(true)

        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `unticking the confirmation blocks submission`() {
        fillCompleteBooking()

        viewModel.onConfirmedChanged(false)

        assertFalse(viewModel.uiState.value.canSubmit)
    }

    // --- Double submit --------------------------------------------------------

    @Test
    fun `a second submit while one is in flight does not reach the server twice`() {
        // The gate is what makes "in flight" real: with an unconfined dispatcher a
        // non-suspending `book` finishes before the next line runs, and this would
        // assert nothing.
        val inFlight = CompletableDeferred<Unit>()
        repository.bookGate = inFlight
        fillCompleteBooking()

        viewModel.submit()
        assertTrue("precondition: the booking is still in flight", viewModel.uiState.value.isSubmitting)

        viewModel.submit()

        // Two POSTs for one tap. The slot is claimed by the first, so the second
        // can only ever come back as a 409 the patient cannot act on.
        assertEquals(1, repository.bookingRequests.size)

        inFlight.complete(Unit)
        assertTrue(viewModel.uiState.value.booked)
    }
}
