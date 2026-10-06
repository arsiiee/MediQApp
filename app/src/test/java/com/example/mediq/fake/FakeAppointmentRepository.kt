package com.example.mediq.fake

import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.RescheduleRequest
import com.example.mediq.domain.repository.AppointmentRepository

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

    // --- What the ViewModel asked for ---------------------------------------

    val requestedIds = mutableListOf<String>()
    val cancelledIds = mutableListOf<String>()
    val rescheduleRequests = mutableListOf<RescheduleRequest>()

    /** True when no read reached the repository at all. */
    val wasCalledAtAll: Boolean get() = requestedIds.isNotEmpty()

    /** True when no mutation reached the repository at all. */
    val wasMutated: Boolean get() = cancelledIds.isNotEmpty() || rescheduleRequests.isNotEmpty()

    override suspend fun getAppointments(filter: AppointmentFilter): Paged<Appointment> =
        Paged(emptyList())

    override suspend fun getAppointment(appointmentId: String): Appointment {
        requestedIds += appointmentId
        getAppointmentError?.let { throw it }
        return appointment
    }

    override suspend fun book(request: BookingRequest): Appointment = appointment

    override suspend fun cancel(appointmentId: String) {
        cancelledIds += appointmentId
        cancelError?.let { throw it }
    }

    override suspend fun requestReschedule(request: RescheduleRequest) {
        rescheduleRequests += request
        requestRescheduleError?.let { throw it }
    }
}
