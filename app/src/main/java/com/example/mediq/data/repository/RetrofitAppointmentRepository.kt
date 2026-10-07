package com.example.mediq.data.repository

import com.example.mediq.data.api.BookingRequestDto
import com.example.mediq.data.api.MediQApiService
import com.example.mediq.data.api.RescheduleRequestDto
import com.example.mediq.data.api.call
import com.example.mediq.data.api.toDomain
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.RescheduleRequest
import com.example.mediq.domain.repository.AppointmentRepository

/**
 * Live implementation of [AppointmentRepository] backed by the Ktor server.
 *
 * List reads (getAppointments) return an empty page when the server is
 * unreachable, so an unreachable backend shows an empty state rather than an
 * error screen. Writes and single-entity reads propagate [ApiFailure] to the
 * ViewModel, carrying the server's own message — "That time was just taken.
 * Please pick another." rather than "HTTP 409 ".
 */
class RetrofitAppointmentRepository(
    private val api: MediQApiService,
) : AppointmentRepository {

    override suspend fun getAppointments(filter: AppointmentFilter): Paged<Appointment> =
        try {
            call { api.getAppointments(filter.wireValue) }.toDomain { toDomain() }
        } catch (e: ApiFailure) {
            // Only an unreachable server becomes an empty page. A 500 or a 409
            // is a real failure and must not be disguised as "no appointments".
            if (e.isNetworkFailure) Paged(emptyList()) else throw e
        }

    override suspend fun getAppointment(appointmentId: String): Appointment =
        call { api.getAppointment(appointmentId) }.toDomain()

    override suspend fun book(request: BookingRequest): Appointment =
        call {
            api.book(
                BookingRequestDto(
                    slotId = request.slotId,
                    reasonForVisit = request.reasonForVisit,
                    confirmedByPatient = request.confirmedByPatient,
                )
            )
        }.toDomain()

    override suspend fun cancel(appointmentId: String) {
        call { api.cancelAppointment(appointmentId) }
    }

    override suspend fun requestReschedule(request: RescheduleRequest) {
        call {
            api.requestReschedule(
                id = request.appointmentId,
                body = RescheduleRequestDto(
                    requestedSlotId = request.requestedSlotId,
                    note = request.note,
                ),
            )
        }
    }
}
