package com.example.mediq.fake

import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentStatus
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.ClinicLocation
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.Money
import com.example.mediq.domain.model.SlotStatus
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.model.UserProfile
import com.example.mediq.domain.model.UserRole
import java.time.Instant
import java.time.LocalDate

/**
 * Builders for the domain objects a test needs.
 *
 * Every field has a default so a test can state only the one value it is
 * actually asserting on. The alternative — spelling out a whole `Appointment` —
 * buries the thing under test in twelve lines of unrelated fixture, and
 * `UnknownWireValueTest` already established the "vary exactly one thing"
 * convention this follows.
 *
 * These are invented on purpose and stay in the test source set. `AGENTS.md`
 * forbids fabricated records in the *app*; a fixture that never leaves
 * `src/test` is not the risk that rule is about.
 */
object TestFixtures {

    fun userProfile(
        fullName: String = "Maria Santos",
        // `UserProfile.email` is a non-null String even though the wire DTO
        // declares it nullable: the server omits nulls (`explicitNulls = false`)
        // and the mapper settles the question before it reaches the domain.
        email: String = "",
    ): UserProfile = UserProfile(
        id = "user-1",
        username = "maria.santos",
        fullName = fullName,
        email = email,
        mobileNumber = "+639175550142",
        dateOfBirth = LocalDate.of(1995, 6, 15),
        sex = null,
        address = null,
        role = UserRole.PATIENT,
    )

    fun authSession(
        profile: UserProfile = userProfile(),
    ): AuthSession = AuthSession(
        accessToken = "test-token",
        expiresAt = Instant.parse("2026-10-06T12:00:00Z"),
        profile = profile,
    )

    fun doctor(
        id: String = "doctor-1",
        fullName: String = "Rivera",
        specialty: Specialty = Specialty.INTERNAL_MEDICINE,
    ): Doctor = Doctor(
        id = id,
        fullName = fullName,
        specialty = specialty,
        yearsOfExperience = 12,
        consultationFee = Money(70000),
        location = ClinicLocation(building = "Main Building", floor = "2F", room = "Clinic 204"),
        licenseNumber = "TEST-PRC-0001",
        bio = "",
        languages = emptyList(),
        clinicHours = emptyList(),
    )

    fun availableDate(
        date: LocalDate = LocalDate.of(2026, 12, 1),
        openSlotCount: Int = 4,
    ): AvailableDate = AvailableDate(date = date, openSlotCount = openSlotCount)

    /**
     * A slot on [date]. Default [SlotStatus.AVAILABLE] so a test that is about
     * availability does not have to say so; a test about a taken slot names the
     * status, which is the one thing it wants to vary.
     */
    fun timeSlot(
        id: String = "slot-1",
        doctorId: String = "doctor-1",
        date: LocalDate = LocalDate.of(2026, 12, 1),
        timeOfDay: String = "01:00:00",
        status: SlotStatus = SlotStatus.AVAILABLE,
    ): TimeSlot {
        // `timeOfDay` is a `LocalTime`-shaped string, so "01:00:00" plus the
        // trailing Z is already an ISO instant — appending another ":00" would
        // produce `T01:00:00:00Z` and throw.
        val startsAt = Instant.parse("${date}T${timeOfDay}Z")
        return TimeSlot(
            id = id,
            doctorId = doctorId,
            startsAt = startsAt,
            endsAt = startsAt.plusSeconds(1800),
            status = status,
        )
    }

    fun appointment(
        id: String = "appointment-1",
        doctor: Doctor = doctor(),
        startsAt: Instant = Instant.parse("2026-12-01T01:00:00Z"),
        status: AppointmentStatus = AppointmentStatus.CONFIRMED,
        reasonForVisit: String? = "Fever and cough",
    ): Appointment = Appointment(
        id = id,
        doctor = doctor,
        startsAt = startsAt,
        endsAt = startsAt.plusSeconds(1800),
        status = status,
        location = ClinicLocation(building = "Main Building", floor = "2F", room = "Clinic 204"),
        fee = Money(70000),
        reasonForVisit = reasonForVisit,
    )
}
