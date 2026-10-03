package com.example.mediq.domain.repository

import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.TimeSlot
import java.time.LocalDate

/**
 * What the app is allowed to ask about doctors and their availability.
 *
 * Nothing implements this yet. When it does, the screens that read doctors
 * should not change — only the object behind it.
 */
interface DoctorRepository {

    suspend fun getDoctors(query: DoctorQuery): Paged<Doctor>

    suspend fun getDoctor(doctorId: String): Doctor

    /** Dates in the given month that have at least one open slot. */
    suspend fun getAvailableDates(doctorId: String, month: LocalDate): List<AvailableDate>

    /** Every slot on one date, including the ones already taken. */
    suspend fun getSlots(doctorId: String, date: LocalDate): List<TimeSlot>
}