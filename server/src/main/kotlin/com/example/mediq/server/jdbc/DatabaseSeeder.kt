package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.security.PasswordHash
import com.zaxxer.hikari.HikariDataSource
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.sql.Connection
import java.sql.Timestamp

class DatabaseSeeder(
    dataSource: HikariDataSource,
    private val passwordHash: PasswordHash,
) : JdbcRepositoryBase(dataSource) {

    fun seedIfEmpty() {
        dataSource.getConnection().use { conn ->
            seedSpecializations(conn)
            seedUsersAndDoctors(conn)
            seedMessages(conn)
        }
    }

    private fun seedSpecializations(conn: Connection) {
        val count = conn.prepareStatement("SELECT COUNT(*) FROM specializations").executeQuery().use { rs -> rs.next(); rs.getLong(1) }
        if (count > 0) return
        val specialties = listOf(
            Specialty("cardiology", "Cardiology", "Heart and cardiovascular system", 0),
            Specialty("dermatology", "Dermatology", "Skin, hair, and nails", 0),
            Specialty("pediatrics", "Pediatrics", "Children's health", 0),
            Specialty("orthopedics", "Orthopedics", "Bones, joints, and muscles", 0),
            Specialty("neurology", "Neurology", "Nervous system disorders", 0),
            Specialty("psychiatry", "Psychiatry", "Mental health", 0),
        )
        conn.prepareStatement("INSERT INTO specializations (id, name, description, doctor_count) VALUES (?, ?, ?, ?)").use { ps ->
            for (s in specialties) {
                ps.setString(1, s.id); ps.setString(2, s.name); ps.setString(3, s.description); ps.setInt(4, s.doctorCount)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun seedUsersAndDoctors(conn: Connection) {
        val count = conn.prepareStatement("SELECT COUNT(*) FROM users").executeQuery().use { rs -> rs.next(); rs.getLong(1) }
        if (count > 0) return
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val demoHash = passwordHash.hash("demo12345")
        val users = listOf(
            UserProfile("doc-1", "dr.smith", "Dr. Sarah Smith", "dr.smith@mediq.app", "+639171234567", LocalDate.parse("1980-05-15"), UserRole.DOCTOR, AccountStatus.ACTIVE, doctorId = "doc-1", createdAt = now, updatedAt = now),
            UserProfile("doc-2", "dr.chen", "Dr. Michael Chen", "dr.chen@mediq.app", "+639172345678", LocalDate.parse("1975-11-22"), UserRole.DOCTOR, AccountStatus.ACTIVE, doctorId = "doc-2", createdAt = now, updatedAt = now),
            UserProfile("sec-1", "sec.jones", "Secretary Jane Jones", "sec.jones@mediq.app", "+639173456789", LocalDate.parse("1990-03-10"), UserRole.SECRETARY, AccountStatus.ACTIVE, assignedDoctorId = "doc-1", createdAt = now, updatedAt = now),
            UserProfile("admin-1", "admin", "Admin User", "admin@mediq.app", "+639170000000", LocalDate.parse("1985-01-01"), UserRole.ADMIN, AccountStatus.ACTIVE, createdAt = now, updatedAt = now),
            UserProfile("doc-3", "dr.garcia", "Dr. Maria Garcia", "dr.garcia@mediq.app", "+639173456789", LocalDate.parse("1982-08-30"), UserRole.DOCTOR, AccountStatus.ACTIVE, doctorId = "doc-3", createdAt = now, updatedAt = now),
            UserProfile("pat-1", "demo_patient", "Demo Patient", "demo@patient.com", "+639181111111", LocalDate.parse("1995-07-20"), UserRole.PATIENT, AccountStatus.ACTIVE, createdAt = now, updatedAt = now),
            UserProfile("pat-2", "demo_patient2", "Maria Santos", "maria@patient.com", "+639182222222", LocalDate.parse("1988-12-05"), UserRole.PATIENT, AccountStatus.ACTIVE, createdAt = now, updatedAt = now),
        )
        conn.autoCommit = false
        try {
            conn.prepareStatement("INSERT INTO users (id, username, full_name, email, mobile_number, date_of_birth, role, status, doctor_id, assigned_doctor_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { userPs ->
                conn.prepareStatement("INSERT INTO password_hashes (user_id, hash) VALUES (?, ?)").use { hashPs ->
                    for (u in users) {
                        userPs.setString(1, u.id); userPs.setString(2, u.username); userPs.setString(3, u.fullName); userPs.setString(4, u.email)
                        userPs.setString(5, u.mobileNumber); userPs.setString(6, u.dateOfBirth.toString()); userPs.setString(7, u.role.name)
                        userPs.setString(8, u.status.name); userPs.setString(9, u.doctorId); userPs.setString(10, u.assignedDoctorId)
                        userPs.setTimestamp(11, toTimestamp(u.createdAt)); userPs.setTimestamp(12, toTimestamp(u.updatedAt)); userPs.addBatch()
                        hashPs.setString(1, u.id); hashPs.setString(2, demoHash); hashPs.addBatch()
                    }
                    userPs.executeBatch(); hashPs.executeBatch()
                }
            }
            val now2 = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val doctors = listOf(
                Doctor("doc-1", "doc-1", "Dr. Sarah Smith", "Cardiology", "cardiology", "PHYS-12345", 15, 150000, "Board-certified cardiologist.", ClinicLocation("MediQ Medical Center", "3", "301"), listOf(ConsultationLanguage("en", "English"), ConsultationLanguage("tl", "Tagalog")), DoctorStatus.ACTIVE, now2, now2),
                Doctor("doc-2", "doc-2", "Dr. Michael Chen", "Dermatology", "dermatology", "PHYS-23456", 12, 120000, "Dermatologist specializing in medical and cosmetic dermatology.", ClinicLocation("MediQ Medical Center", "2", "205"), listOf(ConsultationLanguage("en", "English"), ConsultationLanguage("zh", "Chinese")), DoctorStatus.ACTIVE, now2, now2),
                Doctor("doc-3", "doc-3", "Dr. Maria Garcia", "Pediatrics", "pediatrics", "PHYS-34567", 10, 100000, "Pediatrician with focus on developmental pediatrics.", ClinicLocation("MediQ Medical Center", "1", "110"), listOf(ConsultationLanguage("en", "English"), ConsultationLanguage("tl", "Tagalog"), ConsultationLanguage("es", "Spanish")), DoctorStatus.ACTIVE, now2, now2),
            )
            conn.prepareStatement("INSERT INTO doctors (id, user_id, full_name, specialty, specialization_id, license_number, years_of_experience, fee_centavos, bio, building, floor, room, languages, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { docPs ->
                for (d in doctors) {
                    docPs.setString(1, d.id); docPs.setString(2, d.userId); docPs.setString(3, d.fullName); docPs.setString(4, d.specialty)
                    docPs.setString(5, d.specializationId); docPs.setString(6, d.licenseNumber); docPs.setInt(7, d.yearsOfExperience); docPs.setLong(8, d.feeCentavos)
                    docPs.setString(9, d.bio); docPs.setString(10, d.location.building); docPs.setString(11, d.location.floor); docPs.setString(12, d.location.room)
                    docPs.setString(13, toJson(d.languages)); docPs.setString(14, d.status.name); docPs.setTimestamp(15, toTimestamp(d.createdAt)); docPs.setTimestamp(16, toTimestamp(d.updatedAt)); docPs.addBatch()
                }
                docPs.executeBatch()
            }
            conn.prepareStatement("INSERT INTO clinic_hours (doctor_id, day_of_week, start_time, end_time, is_closed) VALUES (?, ?, ?, ?, ?)").use { schedPs ->
                for (d in doctors) {
                    for (day in 1..5) {
                        schedPs.setString(1, d.id); schedPs.setInt(2, day); schedPs.setString(3, "09:00"); schedPs.setString(4, "17:00"); schedPs.setBoolean(5, false); schedPs.addBatch()
                    }
                }
                schedPs.executeBatch()
            }
            conn.commit()
            println("[MediQ] Seeded demo data. Demo patient: demo_patient / demo12345")
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }

    private fun seedMessages(conn: Connection) {
        val count = conn.prepareStatement("SELECT COUNT(*) FROM message_threads").executeQuery().use { rs -> rs.next(); rs.getLong(1) }
        if (count > 0) return

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val threads = listOf(
            MessageThread(
                id = "conv-1",
                patientId = "pat-1",
                doctorId = "doc-1",
                subject = "Appointment follow-up",
                lastMessageAt = now,
                unreadCount = 1,
                awaitingAction = false,
            ),
            MessageThread(
                id = "conv-2",
                patientId = "pat-2",
                doctorId = "doc-2",
                subject = "Lab results inquiry",
                lastMessageAt = now,
                unreadCount = 0,
                awaitingAction = false,
            ),
        )

        conn.prepareStatement("INSERT INTO message_threads (id, patient_id, doctor_id, subject, last_message_at, unread_count, awaiting_action) VALUES (?, ?, ?, ?, ?, ?, ?)").use { threadPs ->
            for (t in threads) {
                threadPs.setString(1, t.id); threadPs.setString(2, t.patientId); threadPs.setString(3, t.doctorId)
                threadPs.setString(4, t.subject); threadPs.setTimestamp(5, toTimestamp(t.lastMessageAt))
                threadPs.setInt(6, t.unreadCount); threadPs.setBoolean(7, t.awaitingAction); threadPs.addBatch()
            }
            threadPs.executeBatch()
        }

        val messages = listOf(
            Message(
                id = "msg-1", conversationId = "conv-1", authorId = "pat-1",
                authorRole = MessageAuthorRole.PATIENT,
                body = "Good morning! Is Dr. Smith still available this Friday afternoon?",
                sentAt = now, readAt = now,
            ),
            Message(
                id = "msg-2", conversationId = "conv-1", authorId = "sec-1",
                authorRole = MessageAuthorRole.SECRETARY,
                body = "Good morning! Dr. Smith has two open slots on Friday, 1:00 PM and 2:30 PM. You may reserve either one in the app.",
                sentAt = now, readAt = now,
            ),
            Message(
                id = "msg-3", conversationId = "conv-1", authorId = "pat-1",
                authorRole = MessageAuthorRole.PATIENT,
                body = "Thank you! Do I need to bring my previous laboratory results?",
                sentAt = now, readAt = now,
            ),
            Message(
                id = "msg-4", conversationId = "conv-1", authorId = "sec-1",
                authorRole = MessageAuthorRole.SECRETARY,
                body = "Yes please, bring your latest CBC and lipid profile. Arrive 15 minutes before your slot for registration.",
                sentAt = now, readAt = null,
            ),
            Message(
                id = "msg-5", conversationId = "conv-2", authorId = "pat-2",
                authorRole = MessageAuthorRole.PATIENT,
                body = "Hello, I would like to ask about my recent lab results.",
                sentAt = now, readAt = now,
            ),
            Message(
                id = "msg-6", conversationId = "conv-2", authorId = "doc-2",
                authorRole = MessageAuthorRole.DOCTOR,
                body = "Hello! Your results are ready. Let's schedule a follow-up to discuss them.",
                sentAt = now, readAt = null,
            ),
        )

        conn.prepareStatement("INSERT INTO messages (id, conversation_id, author_id, author_role, body, sent_at, read_at) VALUES (?, ?, ?, ?, ?, ?, ?)").use { msgPs ->
            for (m in messages) {
                msgPs.setString(1, m.id); msgPs.setString(2, m.conversationId); msgPs.setString(3, m.authorId)
                msgPs.setString(4, m.authorRole.name); msgPs.setString(5, m.body)
                msgPs.setTimestamp(6, toTimestamp(m.sentAt)); msgPs.setTimestamp(7, m.readAt?.let { toTimestamp(it) })
                msgPs.addBatch()
            }
            msgPs.executeBatch()
        }
    }
}
