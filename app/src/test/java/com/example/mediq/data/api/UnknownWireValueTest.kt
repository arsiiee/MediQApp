package com.example.mediq.data.api

import com.example.mediq.domain.model.AppointmentStatus
import com.example.mediq.domain.model.SlotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant

/**
 * Pins what an unrecognised wire value does.
 *
 * The old behaviour was `firstOrNull { … } ?: <first enum entry>`, which turned
 * a renamed value into a confident wrong answer. The two that mattered:
 *
 *  - an unrecognised appointment status became `PENDING_CONFIRMATION`, so a
 *    cancelled appointment was shown to the patient as awaiting confirmation;
 *  - an unrecognised slot status became `BLOCKED`… and before that change, in
 *    the other direction, an unknown value falling back to `AVAILABLE` would
 *    have offered a slot whose real state was unknown.
 *
 * These fail if someone reintroduces a positional fallback, or adds `UNKNOWN`
 * to an enum where the server would then accept it as valid input.
 */
class UnknownWireValueTest {

    private fun appointmentWithStatus(wire: String) = AppointmentDto(
        id = "a1",
        doctor = DoctorDto(
            id = "d1",
            fullName = "Dr. Santos",
            specialty = "internal_medicine",
            yearsOfExperience = 12,
            consultationFee = MoneyDto(50000),
            location = ClinicLocationDto("Main", "2F", "201"),
            licenseNumber = "DEMO-PRC-0001",
            bio = "",
            languages = listOf("english"),
            clinicHours = emptyList(),
        ),
        startsAt = "2026-11-02T01:30:00Z",
        endsAt = "2026-11-02T02:00:00Z",
        status = wire,
        location = ClinicLocationDto("Main", "2F", "201"),
        fee = MoneyDto(50000),
    )

    private fun slotWithStatus(wire: String) = TimeSlotDto(
        id = "s1",
        doctorId = "d1",
        startsAt = "2026-11-02T01:30:00Z",
        endsAt = "2026-11-02T02:00:00Z",
        status = wire,
    )

    // --- The value must stay unknown ----------------------------------------

    @Test
    fun `an unrecognised appointment status does not become pending confirmation`() {
        val mapped = appointmentWithStatus("awaiting_clinic_review").toDomain()

        assertEquals(AppointmentStatus.UNKNOWN, mapped.status)
        // The specific regression: this used to be PENDING_CONFIRMATION.
        assertFalse(mapped.status == AppointmentStatus.PENDING_CONFIRMATION)
    }

    @Test
    fun `an unrecognised appointment status is neither upcoming nor actionable`() {
        val status = appointmentWithStatus("some_future_state").toDomain().status

        // Offering Cancel on the strength of a guess is how the wrong action
        // gets offered on an appointment whose real state is unknown.
        assertFalse(status.isActionable)
        assertFalse(status.isUpcoming)
    }

    @Test
    fun `an unrecognised slot status is not bookable`() {
        val slot = slotWithStatus("held_for_later").toDomain()

        assertEquals(SlotStatus.UNKNOWN, slot.status)
        assertFalse(slot.isBookable)
    }

    @Test
    fun `unknown still shows as unknown rather than borrowing another label`() {
        assertEquals("Unknown", AppointmentStatus.UNKNOWN.displayName)
    }

    // --- Known values must still resolve normally ---------------------------

    @Test
    fun `a known status is unaffected`() {
        assertEquals(
            AppointmentStatus.CANCELLED,
            appointmentWithStatus("cancelled").toDomain().status,
        )
        assertEquals(
            AppointmentStatus.CONFIRMED,
            appointmentWithStatus("confirmed").toDomain().status,
        )
    }

    @Test
    fun `a known slot status is unaffected`() {
        assertEquals(SlotStatus.AVAILABLE, slotWithStatus("available").toDomain().status)
        assertEquals(SlotStatus.RESERVED, slotWithStatus("reserved").toDomain().status)
    }

    @Test
    fun `a known status still lands in history so it cannot vanish from both tabs`() {
        val appointment = appointmentWithStatus("completed").toDomain()
        assertEquals(
            true,
            appointment.belongsInHistory(Instant.parse("2026-01-01T00:00:00Z")),
        )
    }

    @Test
    fun `an unknown status is kept in history rather than dropped`() {
        // Falling out of both tabs would hide an appointment entirely.
        val appointment = appointmentWithStatus("some_future_state").toDomain()
        assertEquals(
            true,
            appointment.belongsInHistory(Instant.parse("2026-01-01T00:00:00Z")),
        )
    }
}
