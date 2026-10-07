package com.example.mediq.fake

import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.RescheduleRequest
import com.example.mediq.domain.repository.AppointmentRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * A hand-written [AppointmentRepository] for ViewModel tests.
 *
 * Records the ids it was asked for, because the bug this harness now covers is
 * precisely a screen that received an id from its route and never used it — a
 * test that only asserts on the returned state would not notice that, whereas
 * `requestedIds` being empty makes it unmissable.
 *
 * The mutations record what they were asked to change for the same reason: a
 * cancel that reported success without reaching `cancel(appointmentId)` would be
 * indistinguishable to a state-only assertion.
 */
class FakeAppointmentRepository : AppointmentRepository {

    // --- Settable outcomes ---------------------------------------------------

    var appointment: Appointment = TestFixtures.appointment()
    var getAppointmentError: Throwable? = null
    var cancelError: Throwable? = null
    var requestRescheduleError: Throwable? = null

    /** What `getAppointments` returns. Empty by default, so a test must opt in. */
    var appointments: List<Appointment> = emptyList()
    var getAppointmentsError: Throwable? = null
    var nextCursor: String? = null

    /** What `book` returns, and how it can fail. */
    var bookResult: Appointment = TestFixtures.appointment()
    var bookError: Throwable? = null

    /**
     * When set, `getAppointments` waits on this before answering.
     *
     * Same purpose as [bookGate], for the list read: without a real suspension
     * point `HomeViewModel`'s two `init` loads cannot be caught mid-flight, so a
     * lost update from one clobbering the other is unreachable from a test.
     */
    var getAppointmentsGate: CompletableDeferred<Unit>? = null

    /**
     * When set, `book` waits on this before answering.
     *
     * Without it a booking never really suspends, so `isSubmitting` is already
     * `false` by the time the next line of the test runs and a test meant to
     * cover "a second submit while one is in flight" measures the test
     * dispatcher instead of the ViewModel's guard.
     */
    var bookGate: CompletableDeferred<Unit>? = null

    // --- What the ViewModel asked for ---------------------------------------

    val requestedIds = mutableListOf<String>()
    val cancelledIds = mutableListOf<String>()
    val rescheduleRequests = mutableListOf<RescheduleRequest>()

    /**
     * Every list read, in order, carrying the filter it was asked for.
     *
     * A list read and a single-entity read are the two halves of `wasCalledAtAll`
     * below, and they are kept apart because a ViewModel that fetched the wrong
     * one is a bug the other would hide: `HomeViewModel` reads the *list* to find
     * the next appointment, and `AppointmentDetailsViewModel` reads the *single*
     * entity by id.
     */
    val requestedFilters = mutableListOf<AppointmentFilter>()

    /** Every booking, in order, carrying the slot and the reason. */
    val bookingRequests = mutableListOf<BookingRequest>()

    /** True when no read reached the repository at all. */
    val wasCalledAtAll: Boolean get() = requestedIds.isNotEmpty() || requestedFilters.isNotEmpty()

    /** True when no mutation reached the repository at all. */
    val wasMutated: Boolean
        get() = cancelledIds.isNotEmpty() || rescheduleRequests.isNotEmpty() || bookingRequests.isNotEmpty()

    override suspend fun getAppointments(filter: AppointmentFilter): Paged<Appointment> {
        requestedFilters += filter
        getAppointmentsGate?.await()
        getAppointmentsError?.let { throw it }
        return Paged(appointments, nextCursor)
    }

    override suspend fun getAppointment(appointmentId: String): Appointment {
        requestedIds += appointmentId
        getAppointmentError?.let { throw it }
        return appointment
    }

    override suspend fun book(request: BookingRequest): Appointment {
        bookingRequests += request
        bookGate?.await()
        bookError?.let { throw it }
        return bookResult
    }

    override suspend fun cancel(appointmentId: String) {
        cancelledIds += appointmentId
        cancelError?.let { throw it }
    }

    override suspend fun requestReschedule(request: RescheduleRequest) {
        rescheduleRequests += request
        requestRescheduleError?.let { throw it }
    }
}
