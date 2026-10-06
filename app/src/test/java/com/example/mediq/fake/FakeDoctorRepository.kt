package com.example.mediq.fake

import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.repository.DoctorRepository
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

    // --- What the ViewModel asked for ---------------------------------------

    val requestedDoctorIds = mutableListOf<String>()
    val requestedDates = mutableListOf<LocalDate>()

    /** True when no availability read reached the repository at all. */
    val wasCalledAtAll: Boolean get() = requestedDates.isNotEmpty()

    override suspend fun getDoctors(query: DoctorQuery): Paged<Doctor> =
        Paged(emptyList())

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