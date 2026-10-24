package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.example.mediq.util.minusHours
import com.example.mediq.util.plusDays
import com.example.mediq.util.plusMinutes
import com.example.mediq.util.withHour
import com.example.mediq.util.withMinute
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class JdbcAppointmentRepository(dataSource: HikariDataSource) : JdbcRepositoryBase(dataSource), AppointmentRepository {

    override suspend fun findById(id: String): LoadState<Appointment> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM appointments WHERE id = ?").use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                if (rs.next()) LoadState.Success(mapAppointment(rs))
                else LoadState.Error(ApiFailure.notFound("Appointment not found"))
            }
        }
    }

    override suspend fun findAll(filter: AppointmentFilter): LoadState<Paged<Appointment>> = withConnection { conn ->
        val (where, params) = appointmentWhere(filter)
        val countSql = "SELECT COUNT(*) FROM appointments $where"
        val total = conn.prepareStatement(countSql).use { ps ->
            bindParams(ps, params)
            ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
        }
        val dataSql = "SELECT * FROM appointments $where ORDER BY starts_at LIMIT ? OFFSET ?"
        conn.prepareStatement(dataSql).use { ps ->
            bindParams(ps, params)
            ps.setInt(params.size + 1, filter.limit)
            ps.setInt(params.size + 2, filter.offset)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<Appointment>()
                while (rs.next()) list.add(mapAppointment(rs))
                LoadState.Success(Paged(list, total, filter.limit, filter.offset))
            }
        }
    }

    override suspend fun create(appointment: Appointment): LoadState<Appointment> = withConnection { conn ->
        conn.prepareStatement("INSERT INTO appointments (id, patient_id, doctor_id, starts_at, duration_minutes, status, reason, created_at, updated_at, completed_at, outcome) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { ps ->
            ps.setString(1, appointment.id)
            ps.setString(2, appointment.patientId)
            ps.setString(3, appointment.doctorId)
            ps.setTimestamp(4, toTimestamp(appointment.startsAt))
            ps.setInt(5, appointment.durationMinutes)
            ps.setString(6, appointment.status.name)
            ps.setString(7, appointment.reason)
            ps.setTimestamp(8, toTimestamp(appointment.createdAt))
            ps.setTimestamp(9, toTimestamp(appointment.updatedAt))
            ps.setTimestamp(10, appointment.completedAt?.let { toTimestamp(it) })
            ps.setString(11, appointment.outcome?.name)
            ps.executeUpdate()
            LoadState.Success(appointment)
        }
    }

    override suspend fun update(appointment: Appointment): LoadState<Appointment> = withConnection { conn ->
        conn.prepareStatement("UPDATE appointments SET patient_id = ?, doctor_id = ?, starts_at = ?, duration_minutes = ?, status = ?, reason = ?, updated_at = ?, completed_at = ?, outcome = ? WHERE id = ?").use { ps ->
            ps.setString(1, appointment.patientId)
            ps.setString(2, appointment.doctorId)
            ps.setTimestamp(3, toTimestamp(appointment.startsAt))
            ps.setInt(4, appointment.durationMinutes)
            ps.setString(5, appointment.status.name)
            ps.setString(6, appointment.reason)
            ps.setTimestamp(7, toTimestamp(nowUtc()))
            ps.setTimestamp(8, appointment.completedAt?.let { toTimestamp(it) })
            ps.setString(9, appointment.outcome?.name)
            ps.setString(10, appointment.id)
            val updated = ps.executeUpdate()
            if (updated == 0) LoadState.Error(ApiFailure.notFound("Appointment not found"))
            else LoadState.Success(appointment)
        }
    }

    override suspend fun updateStatus(id: String, status: AppointmentStatus, outcome: VisitOutcome?): LoadState<Appointment> = withConnection { conn ->
        val completedAt = if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.NO_SHOW) nowUtc() else null
        conn.prepareStatement("UPDATE appointments SET status = ?, outcome = ?, updated_at = ?, completed_at = ? WHERE id = ?").use { ps ->
            ps.setString(1, status.name)
            ps.setString(2, outcome?.name)
            ps.setTimestamp(3, toTimestamp(nowUtc()))
            ps.setTimestamp(4, completedAt?.let { toTimestamp(it) })
            ps.setString(5, id)
            val updated = ps.executeUpdate()
            if (updated == 0) LoadState.Error(ApiFailure.notFound("Appointment not found"))
            else findById(id)
        }
    }

    override suspend fun delete(id: String): LoadState<Unit> = withConnection { conn ->
        conn.prepareStatement("DELETE FROM appointments WHERE id = ?").use { ps ->
            ps.setString(1, id)
            ps.executeUpdate()
        }
        LoadState.Success(Unit)
    }

    override suspend fun findConflicts(doctorId: String, startsAt: kotlinx.datetime.LocalDateTime, durationMinutes: Int, excludeId: String?): LoadState<List<Appointment>> = withConnection { conn ->
        val endTime = startsAt.plusMinutes(durationMinutes)
        val lowerBound = startsAt.minusHours(24)
        conn.prepareStatement("SELECT * FROM appointments WHERE doctor_id = ? AND status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED') AND starts_at >= ? AND starts_at < ? ORDER BY starts_at").use { ps ->
            ps.setString(1, doctorId)
            ps.setTimestamp(2, toTimestamp(lowerBound))
            ps.setTimestamp(3, toTimestamp(endTime))
            ps.executeQuery().use { rs ->
                val list = mutableListOf<Appointment>()
                while (rs.next()) {
                    val appt = mapAppointment(rs)
                    if (appt.id != excludeId && appt.startsAt < endTime && appt.startsAt.plusMinutes(appt.durationMinutes) > startsAt) {
                        list.add(appt)
                    }
                }
                LoadState.Success(list)
            }
        }
    }

    private fun appointmentWhere(filter: AppointmentFilter): Pair<String, MutableList<Any>> {
        val sql = StringBuilder("WHERE 1 = 1")
        val params = mutableListOf<Any>()
        if (filter.doctorId != null) { sql.append(" AND doctor_id = ?"); params.add(filter.doctorId) }
        if (filter.patientId != null) { sql.append(" AND patient_id = ?"); params.add(filter.patientId) }
        if (filter.status != null) { sql.append(" AND status = ?"); params.add(filter.status.name) }
        if (filter.from != null) { sql.append(" AND starts_at >= ?"); params.add(toTimestamp(filter.from)) }
        if (filter.to != null) { sql.append(" AND starts_at <= ?"); params.add(toTimestamp(filter.to)) }
        return sql.toString() to params
    }

    private fun bindParams(ps: java.sql.PreparedStatement, params: List<Any>) {
        for ((i, p) in params.withIndex()) ps.setObject(i + 1, p)
    }

    private fun mapAppointment(rs: ResultSet): Appointment =
        Appointment(
            id = rs.getString("id"),
            patientId = rs.getString("patient_id"),
            doctorId = rs.getString("doctor_id"),
            startsAt = fromTimestamp(rs.getTimestamp("starts_at"))!!,
            durationMinutes = rs.getInt("duration_minutes"),
            status = AppointmentStatus.valueOf(rs.getString("status")),
            reason = rs.getString("reason"),
            createdAt = fromTimestamp(rs.getTimestamp("created_at"))!!,
            updatedAt = fromTimestamp(rs.getTimestamp("updated_at"))!!,
            completedAt = fromTimestamp(rs.getTimestamp("completed_at")),
            outcome = rs.getString("outcome")?.let { VisitOutcome.valueOf(it) },
        )
}
