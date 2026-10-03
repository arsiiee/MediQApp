package com.example.mediq.domain.repository

import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.RescheduleRequest

/**
 * What the app is allowed to ask about appointments, and what it is allowed to
 * change. Booking, cancelling, and rescheduling are mutations rather than
 * queries, so they are kept here next to the reads that show the result.
 */
interface AppointmentRepository {

    suspend fun getAppointments(filter: AppointmentFilter): Paged<Appointment>

    suspend fun getAppointment(appointmentId: String): Appointment

    /** Returns the appointment the clinic created, with a real id. */
    suspend fun book(request: BookingRequest): Appointment

    suspend fun cancel(appointmentId: String)

    suspend fun requestReschedule(request: RescheduleRequest)
}