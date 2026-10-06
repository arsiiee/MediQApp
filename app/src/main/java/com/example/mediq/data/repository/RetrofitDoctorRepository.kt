package com.example.mediq.data.repository

import com.example.mediq.data.api.MediQApiService
import com.example.mediq.data.api.call
import com.example.mediq.data.api.toDomain
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.repository.DoctorRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Live implementation of [DoctorRepository] backed by the Ktor server.
 *
 * Error-handling follows the same contract as [EmptyDoctorRepository]:
 *  - List reads (getDoctors, getAvailableDates, getSlots) return an empty
 *    result when the server is unreachable, because that is the expected state
 *    during development, not a failure.
 *  - Single-entity reads (getDoctor) let [ApiFailure] propagate so the
 *    DoctorDetailsViewModel can surface it as LoadState.Error.
 */
class RetrofitDoctorRepository(
    private val api: MediQApiService,
) : DoctorRepository {

    override suspend fun getDoctors(query: DoctorQuery): Paged<Doctor> =
        try {
            val params = buildMap<String, String> {
                query.searchText?.let { put("search", it) }
                query.specialty?.let  { put("specialty", it.wireValue) }
                query.building?.let   { put("building", it) }
            }
            call { api.getDoctors(params) }.toDomain { toDomain() }
        } catch (e: ApiFailure) {
            if (e.isNetworkFailure) Paged(emptyList()) else throw e
        }

    override suspend fun getDoctor(doctorId: String): Doctor =
        call { api.getDoctor(doctorId) }.toDomain()

    override suspend fun getAvailableDates(
        doctorId: String,
        month: LocalDate,
    ): List<AvailableDate> =
        try {
            val monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            call { api.getAvailability(doctorId, monthStr) }.items.map { it.toDomain() }
        } catch (e: ApiFailure) {
            if (e.isNetworkFailure) emptyList() else throw e
        }

    override suspend fun getSlots(
        doctorId: String,
        date: LocalDate,
    ): List<TimeSlot> =
        try {
            call { api.getSlots(doctorId, date.toString()) }.items.map { it.toDomain() }
        } catch (e: ApiFailure) {
            if (e.isNetworkFailure) emptyList() else throw e
        }
}
