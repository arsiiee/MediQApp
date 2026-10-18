package com.example.mediq.data.api

import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentStatus
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * What `BOOKED ON` does when the field is present, absent, or unreadable.
 *
 * All three cases go through Gson rather than by constructing an
 * [AppointmentDto] in Kotlin, because the risk this field carries is a *wire*
 * risk: Gson hands back `null` for "the server never sent this" and for "the
 * field was renamed" identically, with no error either way
 * (`GsonLeniencyTest` records the same thing for `DoctorDto`). A test that built
 * the DTO directly would never exercise the path that actually fails.
 *
 * The screen reads `createdAt` and hides the row when it is null, so the whole
 * point of the null cases is that a patient still sees the appointment — its
 * date, time, fee, and status — rather than a screen that failed to render.
 */
class AppointmentCreatedAtTest {

    private val gson = Gson()

    private fun parse(rawCreatedAt: String?): Appointment {
        val createdAtLine = if (rawCreatedAt == null) "" else """"createdAt": "$rawCreatedAt","""
        val json = """
            {
              $createdAtLine
              "id": "a1",
              "doctor": {
                "id": "d1",
                "fullName": "Santos",
                "specialty": "dermatology",
                "yearsOfExperience": 15,
                "consultationFee": { "amountInCentavos": 85000 },
                "location": { "building": "Main Building", "floor": "2F", "room": "Clinic 312" },
                "licenseNumber": "DEMO-PRC-0003",
                "bio": "",
                "languages": [ "english" ],
                "clinicHours": [ { "dayOfWeek": 1, "opensAt": "09:00", "closesAt": "12:00" } ]
              },
              "startsAt": "2026-11-09T01:00:00Z",
              "endsAt": "2026-11-09T01:30:00Z",
              "status": "confirmed",
              "location": { "building": "Main Building", "floor": "2F", "room": "Clinic 312" },
              "fee": { "amountInCentavos": 85000 },
              "reasonForVisit": "Itchy rash on both forearms."
            }
        """.trimIndent()

        return gson.fromJson(json, AppointmentDto::class.java).toDomain()
    }

    @Test
    fun `a booking timestamp on the wire reaches the domain`() {
        val appointment = parse("2026-10-07T01:15:00Z")

        assertEquals(Instant.parse("2026-10-07T01:15:00Z"), appointment.createdAt)
    }

    @Test
    fun `an absent booking timestamp maps to null and the rest of the row still reads`() {
        // The older server, and the renamed field: indistinguishable, both null.
        val appointment = parse(null)

        assertNull(appointment.createdAt)
        assertEquals(AppointmentStatus.CONFIRMED, appointment.status)
        assertEquals(85000L, appointment.fee.amountInCentavos)
    }

    @Test
    fun `a booking timestamp this build cannot read maps to null rather than throwing`() {
        // `Instant.parse` throws on this. The screen must lose one row, not the
        // appointment.
        val appointment = parse("07 Oct 2026")

        assertNull(appointment.createdAt)
        assertEquals(AppointmentStatus.CONFIRMED, appointment.status)
    }
}
