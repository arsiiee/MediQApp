package com.example.mediq.server

import com.example.mediq.domain.model.ClinicHours
import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.model.UserRole
import com.example.mediq.server.db.AppointmentStore
import com.example.mediq.server.db.DoctorStore
import com.example.mediq.server.db.UserRow
import com.example.mediq.server.db.UserStatus
import com.example.mediq.server.db.UserStore
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DoctorWorkspaceTest {

    @Test
    fun `listForDoctor returns only appointments for that doctor ordered by start time`() {
        val f = Fixture()
        val doc1 = f.insertDoctor("Dr. One")
        val doc2 = f.insertDoctor("Dr. Two")
        val patient = f.createPatient("Pat Patient")

        val now = Instant.now()
        f.insertAppointment(patient.id, doc1, now.plusSeconds(3600), "confirmed")
        f.insertAppointment(patient.id, doc1, now.plusSeconds(7200), "pending_confirmation")
        f.insertAppointment(patient.id, doc2, now.plusSeconds(1800), "confirmed")

        val doc1Appts = f.appointments.listForDoctor(doc1)
        assertEquals(2, doc1Appts.size)
        assertEquals(doc1, doc1Appts[0].doctorId)
        assertEquals(doc1, doc1Appts[1].doctorId)
        assertTrue(doc1Appts[0].startsAt <= doc1Appts[1].startsAt)

        val doc2Appts = f.appointments.listForDoctor(doc2)
        assertEquals(1, doc2Appts.size)
        assertEquals(doc2, doc2Appts[0].doctorId)
    }

    @Test
    fun `recordOutcome records completed and no_show on confirmed appointments`() {
        val f = Fixture()
        val doc = f.insertDoctor("Dr. Specialist")
        val patient = f.createPatient("Pat Patient")

        val appt1 = f.insertAppointment(patient.id, doc, Instant.now().plusSeconds(3600), "confirmed")
        val appt2 = f.insertAppointment(patient.id, doc, Instant.now().plusSeconds(7200), "confirmed")

        val res1 = f.appointments.recordOutcome(doc, appt1, "completed")
        assertEquals("completed", res1)

        val res2 = f.appointments.recordOutcome(doc, appt2, "no-show")
        assertEquals("no_show", res2)

        val updated1 = f.appointments.listForDoctor(doc).find { it.id == appt1 }
        assertEquals("completed", updated1?.status)

        val updated2 = f.appointments.listForDoctor(doc).find { it.id == appt2 }
        assertEquals("no_show", updated2?.status)
    }

    @Test
    fun `recordOutcome refuses non-confirmed appointments or appointments for other doctors`() {
        val f = Fixture()
        val doc1 = f.insertDoctor("Dr. One")
        val doc2 = f.insertDoctor("Dr. Two")
        val patient = f.createPatient("Pat Patient")

        val pendingAppt = f.insertAppointment(patient.id, doc1, Instant.now().plusSeconds(3600), "pending_confirmation")
        val err1 = assertFailsWith<ApiError> {
            f.appointments.recordOutcome(doc1, pendingAppt, "completed")
        }
        assertEquals(400, err1.status.value)

        val otherDocAppt = f.insertAppointment(patient.id, doc2, Instant.now().plusSeconds(3600), "confirmed")
        val err2 = assertFailsWith<ApiError> {
            f.appointments.recordOutcome(doc1, otherDocAppt, "completed")
        }
        assertEquals(404, err2.status.value)
    }

    @Test
    fun `listPatientsForDoctor returns only patients who have appointments with that doctor`() {
        val f = Fixture()
        val doc1 = f.insertDoctor("Dr. One")
        val doc2 = f.insertDoctor("Dr. Two")

        val pat1 = f.createPatient("Alice Patient")
        val pat2 = f.createPatient("Bob Patient")
        val pat3 = f.createPatient("Charlie Patient")

        f.insertAppointment(pat1.id, doc1, Instant.now().plusSeconds(3600), "confirmed")
        f.insertAppointment(pat2.id, doc1, Instant.now().plusSeconds(7200), "completed")
        f.insertAppointment(pat3.id, doc2, Instant.now().plusSeconds(1800), "confirmed")

        val doc1Patients = f.users.listPatientsForDoctor(doc1)
        assertEquals(listOf("Alice Patient", "Bob Patient"), doc1Patients.map { it.fullName })

        val doc2Patients = f.users.listPatientsForDoctor(doc2)
        assertEquals(listOf("Charlie Patient"), doc2Patients.map { it.fullName })
    }

    @Test
    fun `updateBio and schedule updates work in DoctorStore`() {
        val f = Fixture()
        val docId = f.insertDoctor("Dr. Test")

        f.doctors.updateBio(docId, "Updated sports medicine bio.")
        val doc = f.doctors.get(docId)
        assertEquals("Updated sports medicine bio.", doc.bio)

        val newSchedule = listOf(
            ClinicHours(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
            ClinicHours(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(17, 0)),
            ClinicHours(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(15, 0)),
        )
        f.doctors.replaceClinicHours(docId, newSchedule)

        val hours = f.doctors.getClinicHours(docId)
        assertEquals(3, hours.size)
        assertEquals(DayOfWeek.MONDAY, hours[0].dayOfWeek)
        assertEquals(LocalTime.of(9, 0), hours[0].opensAt)
        assertEquals(LocalTime.of(12, 0), hours[0].closesAt)
        assertEquals(DayOfWeek.WEDNESDAY, hours[2].dayOfWeek)
    }

    private class Fixture {
        val config = ServerConfig.fromEnv().copy(jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1")
        val db = Database(config)
        val doctors = DoctorStore(db, slotDurationMinutes = 30)
        val appointments = AppointmentStore(db, doctors)
        val users = UserStore(db)

        fun createPatient(name: String): UserRow {
            val username = name.lowercase().replace(" ", "") + "_" + UUID.randomUUID().toString().take(4)
            return users.create(
                username = username,
                password = "Password123!",
                fullName = name,
                email = "$username@example.invalid",
                mobileNumber = mobile(),
                dateOfBirth = LocalDate.of(1990, 5, 12),
                sex = Sex.FEMALE,
                role = UserRole.PATIENT,
            )
        }

        fun insertDoctor(name: String): String {
            val id = UUID.randomUUID().toString()
            db.tx { c ->
                c.prepareStatement("MERGE INTO specialties (id, display_name) KEY (id) VALUES (?, ?)")
                    .use { st ->
                        st.setString(1, Specialty.INTERNAL_MEDICINE.wireValue)
                        st.setString(2, Specialty.INTERNAL_MEDICINE.displayName)
                        st.executeUpdate()
                    }
                c.prepareStatement(
                    """
                    INSERT INTO doctors (id, full_name, specialty_id, years_experience, fee_centavos,
                                         license_number, bio, building, floor, room, languages, created_at)
                    VALUES (?, ?, ?, 8, 80000, 'PRC-TEST', 'Original bio', 'Tower A', '3F', '301', 'en', ?)
                    """.trimIndent()
                ).use { st ->
                    st.setString(1, id)
                    st.setString(2, name)
                    st.setString(3, Specialty.INTERNAL_MEDICINE.wireValue)
                    st.setObject(4, OffsetDateTime.now(ZoneOffset.UTC))
                    st.executeUpdate()
                }
            }
            return id
        }

        fun insertAppointment(patientId: String, doctorId: String, startsAt: Instant, status: String): String {
            val id = UUID.randomUUID().toString()
            val slotId = UUID.randomUUID().toString()
            db.tx { c ->
                c.prepareStatement(
                    """
                    INSERT INTO slots (id, doctor_id, starts_at, ends_at, status)
                    VALUES (?, ?, ?, ?, 'available')
                    """.trimIndent()
                ).use { st ->
                    st.setString(1, slotId)
                    st.setString(2, doctorId)
                    st.setObject(3, startsAt.atOffset(ZoneOffset.UTC))
                    st.setObject(4, startsAt.plusSeconds(1800).atOffset(ZoneOffset.UTC))
                    st.executeUpdate()
                }
                c.prepareStatement(
                    """
                    INSERT INTO appointments (id, user_id, doctor_id, slot_id, starts_at, ends_at,
                                             status, reason_for_visit, confirmed_by_patient, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'Routine checkup', true, ?, ?)
                    """.trimIndent()
                ).use { st ->
                    st.setString(1, id)
                    st.setString(2, patientId)
                    st.setString(3, doctorId)
                    st.setString(4, slotId)
                    st.setObject(5, startsAt.atOffset(ZoneOffset.UTC))
                    st.setObject(6, startsAt.plusSeconds(1800).atOffset(ZoneOffset.UTC))
                    st.setString(7, status)
                    st.setObject(8, OffsetDateTime.now(ZoneOffset.UTC))
                    st.setObject(9, OffsetDateTime.now(ZoneOffset.UTC))
                    st.executeUpdate()
                }
            }
            return id
        }

        private fun mobile(): String {
            val digits = (1_000_000_000L + kotlin.math.abs(UUID.randomUUID().hashCode().toLong()))
                .toString()
                .takeLast(9)
            return "+639$digits"
        }
    }
}
