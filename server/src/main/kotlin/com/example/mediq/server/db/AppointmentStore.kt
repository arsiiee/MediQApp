package com.example.mediq.server.db

import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.AppointmentStatus
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.Money
import com.example.mediq.domain.model.NotificationType
import com.example.mediq.domain.model.RescheduleRequest
import com.example.mediq.domain.model.UserRole
import com.example.mediq.server.ApiError
import com.example.mediq.server.Database
import java.sql.Connection
import java.sql.SQLIntegrityConstraintViolationException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class AppointmentRow(
    val id: String,
    val userId: String,
    val doctorId: String,
    val slotId: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val status: AppointmentStatus,
    val reasonForVisit: String?,
)

/**
 * Appointments, and the booking transaction.
 *
 * Double-booking protection is the database's job, not this class's. Booking
 * inserts a row into `slot_claims`, whose primary key is the slot id. Two
 * simultaneous requests produce two inserts and the second is rejected by the
 * database. A "SELECT then INSERT" written here instead would have a window
 * between the two statements, and both requests would pass.
 */
class AppointmentStore(
    private val db: Database,
    private val doctors: DoctorStore,
) {

    fun list(
        userId: String,
        role: UserRole,
        filter: AppointmentFilter,
        limit: Int,
        offset: Int,
    ): Pair<List<AppointmentRow>, Boolean> = db.read { c ->
        val now = Instant.now()
        val where = StringBuilder(" WHERE 1=1")
        // Staff see the clinic's schedule; patients see only their own.
        if (role == UserRole.PATIENT) where.append(" AND a.user_id = ?")

        when (filter) {
            AppointmentFilter.UPCOMING -> where.append(" AND a.status IN ('pending_confirmation','confirmed') AND a.starts_at >= ?")
            AppointmentFilter.HISTORY -> where.append(" AND (a.status IN ('completed','cancelled','declined') OR a.starts_at < ?)")
            AppointmentFilter.ALL -> Unit
        }

        val params = mutableListOf<Any?>()
        if (role == UserRole.PATIENT) params += userId
        if (filter == AppointmentFilter.UPCOMING) params += now
        if (filter == AppointmentFilter.HISTORY) params += now

        val rows = c.prepareStatement(
            """
            SELECT a.id, a.user_id, a.doctor_id, a.slot_id, a.starts_at, a.ends_at,
                   a.status, a.reason_for_visit
            FROM appointments a
            $where
            ORDER BY a.starts_at ${if (filter == AppointmentFilter.HISTORY) "DESC" else "ASC"}
            LIMIT ? OFFSET ?
            """.trimIndent()
        ).use { st ->
            params.forEachIndexed { i, p -> st.setObject(i + 1, p) }
            st.setInt(params.size + 1, limit + 1)
            st.setInt(params.size + 2, offset)
            st.executeQuery().use { rs -> generateSequence { if (rs.next()) rs.readRow() else null }.toList() }
        }

        rows.take(limit) to (rows.size > limit)
    }

    fun get(userId: String, role: UserRole, appointmentId: String): AppointmentRow = db.read { c ->
        val row = c.prepareStatement("$SELECT WHERE a.id = ?").use { st ->
            st.setString(1, appointmentId)
            st.executeQuery().use { rs -> if (rs.next()) rs.readRow() else null }
        } ?: throw ApiError.notFound("That appointment was not found.")

        if (row.userId != userId && role == UserRole.PATIENT) {
            // Deliberately notFound rather than forbidden: confirming an
            // appointment id exists is itself a disclosure.
            throw ApiError.notFound("That appointment was not found.")
        }
        row
    }

    /**
     * Books [request] for [userId].
     *
     * Everything happens in one transaction. If the notification insert failed
     * after the claim succeeded and there were no transaction, the slot would
     * be marked taken for an appointment the patient never received — and
     * nobody would ever be able to book it again.
     */
    fun book(userId: String, request: BookingRequest): AppointmentRow = db.tx { c ->
        if (!request.confirmedByPatient) {
            throw ApiError.badRequest("You need to confirm the booking details first.")
        }

        val slot = doctors.requireBookableSlot(c, request.slotId)
        val appointmentId = UUID.randomUUID().toString()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        c.prepareStatement(
            """
            INSERT INTO appointments (id, user_id, doctor_id, slot_id, starts_at, ends_at,
                                       status, reason_for_visit, confirmed_by_patient,
                                       created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, 'pending_confirmation', ?, ?, ?, ?)
            """.trimIndent()
        ).use { st ->
            st.setString(1, appointmentId)
            st.setString(2, userId)
            st.setString(3, slot.doctorId)
            st.setString(4, slot.id)
            st.setObject(5, slot.startsAt.atOffset(ZoneOffset.UTC))
            st.setObject(6, slot.endsAt.atOffset(ZoneOffset.UTC))
            st.setString(7, request.reasonForVisit)
            st.setBoolean(8, true)
            st.setObject(9, now)
            st.setObject(10, now)
            st.executeUpdate()
        }

        // The claim is what makes the slot unavailable, and its primary key is
        // the only thing standing between two patients and the same 9:30 AM.
        try {
            c.prepareStatement(
                "INSERT INTO slot_claims (slot_id, appointment_id, claimed_at) VALUES (?, ?, ?)"
            ).use { st ->
                st.setString(1, slot.id)
                st.setString(2, appointmentId)
                st.setObject(3, now)
                st.executeUpdate()
            }
        } catch (e: SQLIntegrityConstraintViolationException) {
            // Someone else booked this slot between the read above and this
            // insert. Rolling back undoes the appointment row too.
            throw ApiError.conflict("That time was just taken. Please pick another.")
        }

        val doctorFirstName = readDoctorBrief(c, slot.doctorId)
        c.prepareStatement(
            """
            INSERT INTO notifications (id, user_id, type, title, body, created_at, related_appointment_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { st ->
            st.setString(1, UUID.randomUUID().toString())
            st.setString(2, userId)
            st.setString(3, NotificationType.APPOINTMENT_CONFIRMED.wireValue)
            st.setString(4, "Appointment requested")
            st.setString(
                5,
                "We received your request for $doctorFirstName on ${slot.startsAt.atZone(com.example.mediq.domain.model.CLINIC_ZONE).toLocalDate()}. " +
                    "The clinic will confirm shortly.",
            )
            st.setObject(6, now)
            st.setString(7, appointmentId)
            st.executeUpdate()
        }

        readRow(c.prepareStatement("$SELECT WHERE a.id = ?").also { it.setString(1, appointmentId) })!!
    }

    /**
     * Cancels an appointment and releases its slot in the same transaction.
     *
     * The appointment row is kept, because a cancelled appointment is part of
     * the patient's history and may need to be produced on request.
     */
    fun cancel(userId: String, role: UserRole, appointmentId: String) = db.tx { c ->
        val appointment = get(userId, role, appointmentId)

        if (!appointment.status.isActionable) {
            throw ApiError.badRequest("That appointment can no longer be cancelled.")
        }
        if (appointment.startsAt.isBefore(Instant.now())) {
            throw ApiError.badRequest("That appointment has already taken place.")
        }

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        c.prepareStatement("UPDATE appointments SET status = 'cancelled', updated_at = ? WHERE id = ?")
            .use { st ->
                st.setObject(1, now)
                st.setString(2, appointmentId)
                st.executeUpdate()
            }

        // Deleting the claim is what frees the slot. Until this row is gone,
        // the slot stays reserved and cannot be booked again.
        c.prepareStatement("DELETE FROM slot_claims WHERE appointment_id = ?").use { st ->
            st.setString(1, appointmentId)
            st.executeUpdate()
        }

        c.prepareStatement(
            """
            INSERT INTO notifications (id, user_id, type, title, body, created_at, related_appointment_id)
            VALUES (?, ?, 'appointment_cancelled', 'Appointment cancelled',
                    'Your appointment has been cancelled and the time slot is available again.', ?, ?)
            """.trimIndent()
        ).use { st ->
            st.setString(1, UUID.randomUUID().toString())
            st.setString(2, appointment.userId)
            st.setObject(3, now)
            st.setString(4, appointmentId)
            st.executeUpdate()
        }
    }

    /**
     * Records what the patient asked for. This does not move the appointment —
     * a secretary has to approve it, and the original slot stays claimed until
     * they do.
     */
    fun requestReschedule(userId: String, role: UserRole, request: RescheduleRequest) = db.tx { c ->
        val appointment = get(userId, role, request.appointmentId)

        if (!appointment.status.isActionable) {
            throw ApiError.badRequest("That appointment can no longer be rescheduled.")
        }
        doctors.requireBookableSlot(c, request.requestedSlotId)

        c.prepareStatement(
            """
            UPDATE appointments
            SET requested_slot_id = ?, reschedule_note = ?, updated_at = ?
            WHERE id = ?
            """.trimIndent()
        ).use { st ->
            st.setString(1, request.requestedSlotId)
            st.setString(2, request.note)
            st.setObject(3, OffsetDateTime.now(ZoneOffset.UTC))
            st.setString(4, request.appointmentId)
            st.executeUpdate()
        }

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        c.prepareStatement(
            """
            INSERT INTO notifications (id, user_id, type, title, body, created_at, related_appointment_id)
            VALUES (?, ?, 'system', 'Reschedule requested',
                    'Your request to move this appointment has been sent to the clinic.', ?, ?)
            """.trimIndent()
        ).use { st ->
            st.setString(1, UUID.randomUUID().toString())
            st.setString(2, appointment.userId)
            st.setObject(3, now)
            st.setString(4, appointment.id)
            st.executeUpdate()
        }
    }

    /** Builds the full appointment, including its doctor, for the client. */
    fun toAppointment(row: AppointmentRow, withReason: Boolean): Appointment {
        val doctor = doctors.get(row.doctorId)
        return Appointment(
            id = row.id,
            doctor = doctor,
            startsAt = row.startsAt,
            endsAt = row.endsAt,
            status = row.status,
            location = doctor.location,
            fee = Money(doctor.consultationFee.amountInCentavos),
            reasonForVisit = row.reasonForVisit.takeIf { withReason },
        )
    }

    private fun readDoctorBrief(c: Connection, doctorId: String): String =
        c.prepareStatement("SELECT full_name FROM doctors WHERE id = ?").use { st ->
            st.setString(1, doctorId)
            st.executeQuery().use { rs -> if (rs.next()) rs.getString("full_name") else "the doctor" }
        }.let { fullName ->
            // First name only, for a one-line notification.
            fullName.substringBefore(' ')
        }

    private fun readRow(st: java.sql.PreparedStatement): AppointmentRow? =
        st.executeQuery().use { rs -> if (rs.next()) rs.readRow() else null }

    private companion object {
        const val SELECT = """
            SELECT a.id, a.user_id, a.doctor_id, a.slot_id, a.starts_at, a.ends_at,
                   a.status, a.reason_for_visit
            FROM appointments a
        """
    }
}

private fun java.sql.ResultSet.readRow() = AppointmentRow(
    id = getString("id"),
    userId = getString("user_id"),
    doctorId = getString("doctor_id"),
    slotId = getString("slot_id"),
    startsAt = getObject("starts_at", OffsetDateTime::class.java).toInstant(),
    endsAt = getObject("ends_at", OffsetDateTime::class.java).toInstant(),
    status = AppointmentStatus.entries.first { it.wireValue == getString("status") },
    reasonForVisit = getString("reason_for_visit"),
)