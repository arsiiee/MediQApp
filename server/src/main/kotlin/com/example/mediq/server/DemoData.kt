package com.example.mediq.server

import com.example.mediq.domain.model.ConsultationLanguage
import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.model.Specialty
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Optional demonstration data, off unless MEDIQ_SEED_DEMO=true.
 *
 * The app has no fake doctors or patients in its own source, and this does not
 * change that — the names below are obviously invented, they exist only so
 * there is something for the screens to render while wiring them to a real
 * server, and the licence numbers are deliberately not valid formats so they
 * can never be mistaken for a real doctor's.
 *
 * Never enable this against a database that holds real patients.
 */
object DemoData {

    fun seed(db: Database) {
        if (db.read { c ->
                c.prepareStatement("SELECT COUNT(*) FROM doctors").use { st ->
                    st.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
                }
            } > 0
        ) {
            println("[MediQ] Demo data already present, skipping.")
            return
        }

        println("[MediQ] Seeding DEMO data — doctors below are invented, not real clinicians.")

        db.tx { c ->
            val doctors = listOf(
                DoctorSeed(
                    name = "Demo Rivera",
                    specialty = Specialty.INTERNAL_MEDICINE,
                    years = 12,
                    fee = 70000L,
                    building = "Main Building",
                    floor = "2F",
                    room = "Clinic 204",
                    licence = "DEMO-PRC-0001",
                    bio = "DEMO RECORD. General adult medicine, chronic conditions, and follow-up care.",
                    languages = listOf(ConsultationLanguage.ENGLISH, ConsultationLanguage.FILIPINO),
                ),
                DoctorSeed(
                    name = "Sample Santos",
                    specialty = Specialty.PEDIATRICS,
                    years = 8,
                    fee = 60000L,
                    building = "Main Building",
                    floor = "2F",
                    room = "Clinic 205",
                    licence = "DEMO-PRC-0002",
                    bio = "DEMO RECORD. Care for infants, children, and adolescents, including immunisation.",
                    languages = listOf(ConsultationLanguage.ENGLISH, ConsultationLanguage.FILIPINO, ConsultationLanguage.CEBUANO),
                ),
                DoctorSeed(
                    name = "Placeholder Cruz",
                    specialty = Specialty.DERMATOLOGY,
                    years = 15,
                    fee = 80000L,
                    building = "Annex",
                    floor = "1F",
                    room = "Clinic 110",
                    licence = "DEMO-PRC-0003",
                    bio = "DEMO RECORD. Skin conditions, rash assessment, and dermatologic screening.",
                    languages = listOf(ConsultationLanguage.ENGLISH),
                ),
            )

            c.prepareStatement(
                """
                INSERT INTO doctors (id, full_name, specialty_id, years_experience, fee_centavos,
                                     license_number, bio, building, floor, room, languages, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { st ->
                doctors.forEach { d ->
                    st.setString(1, UUID.randomUUID().toString())
                    st.setString(2, d.name)
                    st.setString(3, d.specialty.wireValue)
                    st.setInt(4, d.years)
                    st.setLong(5, d.fee)
                    st.setString(6, d.licence)
                    st.setString(7, d.bio)
                    st.setString(8, d.building)
                    st.setString(9, d.floor)
                    st.setString(10, d.room)
                    st.setString(11, d.languages.joinToString(",") { it.wireValue })
                    st.setObject(12, OffsetDateTime.now(ZoneOffset.UTC))
                    st.addBatch()
                }
                st.executeBatch()
            }

            // Weekday mornings and afternoons for every demo doctor.
            //
            // The id is generated in SQL rather than bound as a parameter: this
            // statement fans out to one row per doctor, so a single bound id
            // would be inserted several times and trip the primary key.
            c.prepareStatement(
                """
                INSERT INTO clinic_hours (id, doctor_id, day_of_week, opens_at, closes_at)
                SELECT RANDOM_UUID(), d.id, ?, ?, ? FROM doctors d
                """.trimIndent()
            ).use { st ->
                DayOfWeek.entries.forEach { day ->
                    if (day == DayOfWeek.SUNDAY) return@forEach
                    listOf(
                        LocalTime.of(9, 0) to LocalTime.of(12, 0),
                        LocalTime.of(14, 0) to LocalTime.of(17, 0),
                    ).forEach { (opens, closes) ->
                        st.setInt(1, day.value)
                        st.setObject(2, opens)
                        st.setObject(3, closes)
                        st.addBatch()
                    }
                }
                st.executeBatch()
            }
        }

        // A demo patient, so sign-in can be tried end to end. Password is
        // printed once and is deliberately weak.
        val password = "demo12345"
        db.tx { c ->
            if (alreadyExists(c, "demo_patient")) return@tx
            c.prepareStatement(
                """
                INSERT INTO users (id, username, password_hash, full_name, email, mobile_number,
                                   date_of_birth, sex, address, role, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'patient', ?)
                """.trimIndent()
            ).use { st ->
                st.setString(1, UUID.randomUUID().toString())
                st.setString(2, "demo_patient")
                st.setString(3, com.example.mediq.server.auth.Passwords.hash(password))
                st.setString(4, "Demo Patient")
                st.setString(5, "demo@example.invalid")
                st.setString(6, "+639000000000")
                st.setObject(7, java.time.LocalDate.of(1990, 1, 1))
                st.setString(8, Sex.PREFER_NOT_TO_SAY.wireValue)
                st.setString(9, null)
                st.setObject(10, OffsetDateTime.now(ZoneOffset.UTC))
                st.executeUpdate()
            }
        }
        println("[MediQ] Demo patient: demo_patient / $password")
    }

    private fun alreadyExists(c: java.sql.Connection, username: String): Boolean =
        c.prepareStatement("SELECT 1 FROM users WHERE username = ?").use { st ->
            st.setString(1, username)
            st.executeQuery().use { rs -> rs.next() }
        }

    private data class DoctorSeed(
        val name: String,
        val specialty: Specialty,
        val years: Int,
        val fee: Long,
        val building: String,
        val floor: String,
        val room: String,
        val licence: String,
        val bio: String,
        val languages: List<ConsultationLanguage>,
    )
}
