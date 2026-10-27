package com.example.mediq.ui.viewmodel

import com.example.mediq.data.model.Appointment
import com.example.mediq.data.model.AppointmentStatus
import com.example.mediq.data.model.ClinicLocation
import com.example.mediq.data.model.Doctor
import com.example.mediq.data.model.DoctorStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import com.example.mediq.ui.session.DateTimeFormatter
import com.example.mediq.ui.screens.home.AppointmentStatus as HomeAppointmentStatus
import com.example.mediq.ui.viewmodel.badgeLabel
import com.example.mediq.ui.viewmodel.nextAppointment

class HomeViewModelTest {

    private val now = DateTimeFormatter.instant("2026-10-27T10:00:00")

    private fun clinic(building: String = "Main Building",
                       floor: String = "2",
                       room: String = "204") =
        ClinicLocation(building, floor, room)

    private fun doctor(
        id: String = "d1",
        fullName: String = "Dr. Maria Sandoval",
        specialty: String = "Internal Medicine",
        location: ClinicLocation = clinic()
    ) = Doctor(
        id = id,
        userId = id,
        fullName = fullName,
        specialty = specialty,
        specializationId = "s1",
        licenseNumber = "L-1",
        yearsOfExperience = 5,
        feeCentavos = 50000,
        bio = "",
        location = location,
        languages = emptyList(),
        status = DoctorStatus.ACTIVE,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    private fun appointment(
        id: String,
        startsAt: String,
        status: AppointmentStatus,
        doctorId: String = "d1"
    ) = Appointment(
        id = id,
        patientId = "p1",
        doctorId = doctorId,
        startsAt = startsAt,
        durationMinutes = 30,
        status = status,
        reason = "Check-up",
        createdAt = "2026-10-26T00:00:00Z",
        updatedAt = "2026-10-26T00:00:00Z"
    )

    @Test
    fun `an upcoming confirmed appointment is shown in 12-hour time and short date`() {
        val result = nextAppointment(
            appointments = listOf(appointment("a1", "2026-10-27T14:00:00", AppointmentStatus.CONFIRMED)),
            doctors = listOf(doctor()),
            now = now
        )

        assertEquals("2:00 PM", result?.time)
        assertEquals("Tue, Oct 27", result?.date)
        assertEquals(HomeAppointmentStatus.Confirmed, result?.status)
        assertEquals("Dr. Maria Sandoval", result?.doctorName)
        assertEquals("Internal Medicine", result?.specialty)
        assertEquals("Main Building · Floor 2 · Room 204", result?.location)
        assertEquals("a1", result?.id)
    }

    @Test
    fun `a booked appointment maps to pending`() {
        val result = nextAppointment(
            appointments = listOf(appointment("a1", "2026-10-27T14:00:00", AppointmentStatus.BOOKED)),
            doctors = listOf(doctor()),
            now = now
        )
        assertEquals(HomeAppointmentStatus.Pending, result?.status)
    }

    @Test
    fun `a past appointment is excluded`() {
        val result = nextAppointment(
            appointments = listOf(appointment("a1", "2026-10-26T14:00:00", AppointmentStatus.CONFIRMED)),
            doctors = listOf(doctor()),
            now = now
        )
        assertNull(result)
    }

    @Test
    fun `an appointment starting exactly now is included`() {
        val result = nextAppointment(
            appointments = listOf(appointment("a1", "2026-10-27T10:00:00", AppointmentStatus.CONFIRMED)),
            doctors = listOf(doctor()),
            now = now
        )
        assertEquals("10:00 AM", result?.time)
    }

    @Test
    fun `a cancelled appointment is excluded`() {
        val result = nextAppointment(
            appointments = listOf(appointment("a1", "2026-10-28T14:00:00", AppointmentStatus.CANCELLED)),
            doctors = listOf(doctor()),
            now = now
        )
        assertNull(result)
    }

    @Test
    fun `a completed appointment is excluded`() {
        val result = nextAppointment(
            appointments = listOf(appointment("a1", "2026-10-28T14:00:00", AppointmentStatus.COMPLETED)),
            doctors = listOf(doctor()),
            now = now
        )
        assertNull(result)
    }

    @Test
    fun `a no-show appointment is excluded`() {
        val result = nextAppointment(
            appointments = listOf(appointment("a1", "2026-10-28T14:00:00", AppointmentStatus.NO_SHOW)),
            doctors = listOf(doctor()),
            now = now
        )
        assertNull(result)
    }

    @Test
    fun `no appointments returns null`() {
        val result = nextAppointment(
            appointments = emptyList(),
            doctors = emptyList(),
            now = now
        )
        assertNull(result)
    }

    @Test
    fun `the earliest upcoming appointment is picked`() {
        val result = nextAppointment(
            appointments = listOf(
                appointment("a2", "2026-10-28T14:00:00", AppointmentStatus.CONFIRMED),
                appointment("a1", "2026-10-27T14:00:00", AppointmentStatus.CONFIRMED)
            ),
            doctors = listOf(doctor()),
            now = now
        )
        assertEquals("a1", result?.id)
        assertEquals("2:00 PM", result?.time)
    }

    @Test
    fun `a missing room is skipped in the location`() {
        val result = nextAppointment(
            appointments = listOf(appointment("a1", "2026-10-27T14:00:00", AppointmentStatus.CONFIRMED)),
            doctors = listOf(doctor(fullName = "Dr. Grace Villanueva", specialty = "OB-Gynecology",
                location = clinic("Annex Wing", "4", ""))),
            now = now
        )
        assertEquals("Annex Wing · Floor 4", result?.location)
    }

    // badgeLabel

    @Test
    fun `badgeLabel hides zero`() {
        assertNull(badgeLabel(0))
    }

    @Test
    fun `badgeLabel shows small counts`() {
        assertEquals("1", badgeLabel(1))
        assertEquals("9", badgeLabel(9))
    }

    @Test
    fun `badgeLabel caps at 9+`() {
        assertEquals("9+", badgeLabel(10))
        assertEquals("9+", badgeLabel(99))
    }

    @Test
    fun `badgeLabel hides negative counts`() {
        assertNull(badgeLabel(-1))
    }
}
