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
 * Only the single-entity read is given a settable result. The interface needs the
 * other methods implemented, so they satisfy the contract and nothing more; when
 * a test needs them, give them a settable result then.
 */
class FakeAppointmentRepository : AppointmentRepository {

    // --- Settable outcomes ---------------------------------------------------

    var appointment: Appointment = TestFixtures.appointment()
    var getAppointmentError: Throwable? = null

    // --- What the ViewModel asked for ---------------------------------------

    val requestedIds = mutableListOf<String>()

    /** True when no read reached the repository at all. */
    val wasCalledAtAll: Boolean get() = requestedIds.isNotEmpty()

    override suspend fun getAppointments(filter: AppointmentFilter): Paged<Appointment> =
        Paged(emptyList())

    override suspend fun getAppointment(appointmentId: String): Appointment {
        requestedIds += appointmentId
        getAppointmentError?.let { throw it }
        return appointment
    }

    override suspend fun book(request: BookingRequest): Appointment = appointment

    override suspend fun cancel(appointmentId: String) = Unit

    override suspend fun requestReschedule(request: RescheduleRequest) = Unit
}
