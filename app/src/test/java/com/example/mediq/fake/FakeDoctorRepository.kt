package com.example.mediq.fake

import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.repository.DoctorRepository
import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDate

/**
 * A hand-written [DoctorRepository] for ViewModel tests.
 *
 * Availability is settable rather than computed, because the thing under test is
 * which dates and slots the ViewModel asks for and which of them it offers — not
 * how the server derives a schedule from `clinic_hours`.
 *
 * Records the dates it was asked about, so a picker that never reached the
 * repository cannot pass on a state-only assertion.
 */
class FakeDoctorRepository : DoctorRepository {

    // --- Settable outcomes ---------------------------------------------------

    var doctor: Doctor = TestFixtures.doctor()
    var getDoctorError: Throwable? = null
    var availableDates: List<AvailableDate> = emptyList()
    var availableDatesError: Throwable? = null
    var slots: List<TimeSlot> = emptyList()
    var slotsError: Throwable? = null

    /** What `getDoctors` returns. Empty by default, so a test must opt in. */
    var doctors: List<Doctor> = emptyList()
    var getDoctorsError: Throwable? = null
    var nextCursor: String? = null

    // --- What the ViewModel asked for ---------------------------------------

    val requestedDoctorIds = mutableListOf<String>()
    val requestedDates = mutableListOf<LocalDate>()

    /**
     * Every `getDoctors` query, in order.
     *
     * A separate list from [requestedDoctorIds] on purpose: that one records
     * *which* doctor an availability read was about, and mixing a list query in
     * would make `requestedDoctorIds.contains(doctorId)` — the assertion the
     * appointment-details tests use — pass for a query that never fetched
     * anything.
     */
    val requestedQueries = mutableListOf<DoctorQuery>()

    /** True when no availability read reached the repository at all. */
    val wasCalledAtAll: Boolean get() = requestedDates.isNotEmpty()

    /** True when no doctor list query reached the repository at all. */
    val wasQueriedAtAll: Boolean get() = requestedQueries.isNotEmpty()

    /**
     * When set, `getDoctors` waits on this before answering.
     *
     * Without it a fake read never really suspends, so `Loading` exists for no
     * observable instant: the ViewModel sets it and replaces it with `Success`
     * inside the same dispatcher pass, and a test asserting on the in-flight
     * state is asserting on something unreachable. Completing the gate is what
     * makes the request genuinely in flight.
     */
    var getDoctorsGate: CompletableDeferred<Unit>? = null

    override suspend fun getDoctors(query: DoctorQuery): Paged<Doctor> {
        requestedQueries += query
        getDoctorsGate?.await()
        getDoctorsError?.let { throw it }
        return Paged(doctors, nextCursor)
    }

    override suspend fun getDoctor(doctorId: String): Doctor {
        requestedDoctorIds += doctorId
        getDoctorError?.let { throw it }
        return doctor
    }

    override suspend fun getAvailableDates(doctorId: String, month: LocalDate): List<AvailableDate> {
        requestedDoctorIds += doctorId
        return availableDates.also { availableDatesError?.let { e -> throw e } }
    }

    override suspend fun getSlots(doctorId: String, date: LocalDate): List<TimeSlot> {
        requestedDoctorIds += doctorId
        requestedDates += date
        return slots.also { slotsError?.let { e -> throw e } }
    }
}