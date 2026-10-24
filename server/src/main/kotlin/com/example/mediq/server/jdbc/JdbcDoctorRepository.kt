package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class JdbcDoctorRepository(dataSource: HikariDataSource) : JdbcRepositoryBase(dataSource), DoctorRepository {

    override suspend fun findById(id: String): LoadState<Doctor> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM doctors WHERE id = ?").use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                if (rs.next()) LoadState.Success(mapDoctor(rs))
                else LoadState.Error(ApiFailure.notFound("Doctor not found"))
            }
        }
    }

    override suspend fun findByUserId(userId: String): LoadState<Doctor?> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM doctors WHERE user_id = ?").use { ps ->
            ps.setString(1, userId)
            ps.executeQuery().use { rs ->
                LoadState.Success(if (rs.next()) mapDoctor(rs) else null)
            }
        }
    }

    override suspend fun findAll(query: DoctorQuery): LoadState<Paged<Doctor>> = withConnection { conn ->
        val (where, params) = doctorWhere(query)
        val countSql = "SELECT COUNT(*) FROM doctors $where"
        val total = conn.prepareStatement(countSql).use { ps ->
            bindParams(ps, params)
            ps.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
        }
        val dataSql = "SELECT * FROM doctors $where ORDER BY full_name LIMIT ? OFFSET ?"
        conn.prepareStatement(dataSql).use { ps ->
            bindParams(ps, params)
            ps.setInt(params.size + 1, query.limit)
            ps.setInt(params.size + 2, query.offset)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<Doctor>()
                while (rs.next()) list.add(mapDoctor(rs))
                LoadState.Success(Paged(list, total, query.limit, query.offset))
            }
        }
    }

    override suspend fun create(doctor: Doctor): LoadState<Doctor> = withConnection { conn ->
        conn.prepareStatement("INSERT INTO doctors (id, user_id, full_name, specialty, specialization_id, license_number, years_of_experience, fee_centavos, bio, building, floor, room, languages, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { ps ->
            ps.setString(1, doctor.id)
            ps.setString(2, doctor.userId)
            ps.setString(3, doctor.fullName)
            ps.setString(4, doctor.specialty)
            ps.setString(5, doctor.specializationId)
            ps.setString(6, doctor.licenseNumber)
            ps.setInt(7, doctor.yearsOfExperience)
            ps.setLong(8, doctor.feeCentavos)
            ps.setString(9, doctor.bio)
            ps.setString(10, doctor.location.building)
            ps.setString(11, doctor.location.floor)
            ps.setString(12, doctor.location.room)
            ps.setString(13, toJson(doctor.languages))
            ps.setString(14, doctor.status.name)
            ps.setTimestamp(15, toTimestamp(doctor.createdAt))
            ps.setTimestamp(16, toTimestamp(doctor.updatedAt))
            ps.executeUpdate()
            LoadState.Success(doctor)
        }
    }

    override suspend fun update(doctor: Doctor): LoadState<Doctor> = withConnection { conn ->
        conn.prepareStatement("UPDATE doctors SET user_id = ?, full_name = ?, specialty = ?, specialization_id = ?, license_number = ?, years_of_experience = ?, fee_centavos = ?, bio = ?, building = ?, floor = ?, room = ?, languages = ?, status = ?, updated_at = ? WHERE id = ?").use { ps ->
            ps.setString(1, doctor.userId)
            ps.setString(2, doctor.fullName)
            ps.setString(3, doctor.specialty)
            ps.setString(4, doctor.specializationId)
            ps.setString(5, doctor.licenseNumber)
            ps.setInt(6, doctor.yearsOfExperience)
            ps.setLong(7, doctor.feeCentavos)
            ps.setString(8, doctor.bio)
            ps.setString(9, doctor.location.building)
            ps.setString(10, doctor.location.floor)
            ps.setString(11, doctor.location.room)
            ps.setString(12, toJson(doctor.languages))
            ps.setString(13, doctor.status.name)
            ps.setTimestamp(14, toTimestamp(nowUtc()))
            ps.setString(15, doctor.id)
            val updated = ps.executeUpdate()
            if (updated == 0) LoadState.Error(ApiFailure.notFound("Doctor not found"))
            else LoadState.Success(doctor.copy(updatedAt = nowUtc()))
        }
    }

    override suspend fun updateStatus(id: String, status: DoctorStatus): LoadState<Doctor> = withConnection { conn ->
        conn.prepareStatement("UPDATE doctors SET status = ?, updated_at = ? WHERE id = ?").use { ps ->
            ps.setString(1, status.name)
            ps.setTimestamp(2, toTimestamp(nowUtc()))
            ps.setString(3, id)
            val updated = ps.executeUpdate()
            if (updated == 0) LoadState.Error(ApiFailure.notFound("Doctor not found"))
            else findById(id)
        }
    }

    override suspend fun findBySpecialization(specializationId: String): LoadState<List<Doctor>> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM doctors WHERE specialization_id = ? ORDER BY full_name").use { ps ->
            ps.setString(1, specializationId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<Doctor>()
                while (rs.next()) list.add(mapDoctor(rs))
                LoadState.Success(list)
            }
        }
    }

    override suspend fun getSchedule(doctorId: String): LoadState<List<ClinicHours>> = withConnection { conn ->
        conn.prepareStatement("SELECT day_of_week, start_time, end_time, is_closed FROM clinic_hours WHERE doctor_id = ? ORDER BY day_of_week, start_time").use { ps ->
            ps.setString(1, doctorId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<ClinicHours>()
                while (rs.next()) list.add(ClinicHours(rs.getInt("day_of_week"), rs.getString("start_time"), rs.getString("end_time"), rs.getBoolean("is_closed")))
                LoadState.Success(list)
            }
        }
    }

    override suspend fun saveSchedule(doctorId: String, schedule: List<ClinicHours>): LoadState<Unit> = withConnection { conn ->
        conn.autoCommit = false
        try {
            conn.prepareStatement("DELETE FROM clinic_hours WHERE doctor_id = ?").use { ps ->
                ps.setString(1, doctorId)
                ps.executeUpdate()
            }
            for (hours in schedule) {
                conn.prepareStatement("INSERT INTO clinic_hours (doctor_id, day_of_week, start_time, end_time, is_closed) VALUES (?, ?, ?, ?, ?)").use { ps ->
                    ps.setString(1, doctorId)
                    ps.setInt(2, hours.dayOfWeek)
                    ps.setString(3, hours.startTime)
                    ps.setString(4, hours.endTime)
                    ps.setBoolean(5, hours.isClosed)
                    ps.executeUpdate()
                }
            }
            conn.commit()
            LoadState.Success(Unit)
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }

    override suspend fun getAvailability(doctorId: String, date: kotlinx.datetime.LocalDate): LoadState<List<AvailableDate>> = withConnection { conn ->
        conn.prepareStatement("SELECT sessions, is_override FROM availabilities WHERE doctor_id = ? AND date = ?").use { ps ->
            ps.setString(1, doctorId)
            ps.setString(2, toDateString(date))
            ps.executeQuery().use { rs ->
                val list = mutableListOf<AvailableDate>()
                while (rs.next()) {
                    list.add(AvailableDate(
                        date = date,
                        sessions = fromJson<List<TimeSlot>>(rs.getString("sessions")) ?: emptyList(),
                        isOverride = rs.getBoolean("is_override"),
                    ))
                }
                LoadState.Success(list)
            }
        }
    }

    override suspend fun saveAvailability(doctorId: String, availability: AvailableDate): LoadState<Unit> = withConnection { conn ->
        conn.prepareStatement("INSERT INTO availabilities (doctor_id, date, sessions, is_override) VALUES (?, ?, ?, ?)").use { ps ->
            ps.setString(1, doctorId)
            ps.setString(2, toDateString(availability.date))
            ps.setString(3, toJson(availability.sessions))
            ps.setBoolean(4, availability.isOverride)
            ps.executeUpdate()
        }
        LoadState.Success(Unit)
    }

    private fun doctorWhere(query: DoctorQuery): Pair<String, MutableList<Any>> {
        val sql = StringBuilder("WHERE 1 = 1")
        val params = mutableListOf<Any>()
        if (query.specializationId != null) { sql.append(" AND specialization_id = ?"); params.add(query.specializationId) }
        if (query.search != null) { sql.append(" AND (full_name ILIKE ? OR specialty ILIKE ?)"); params.add("%${query.search}%"); params.add("%${query.search}%") }
        if (query.status != null) { sql.append(" AND status = ?"); params.add(query.status.name) }
        return sql.toString() to params
    }

    private fun bindParams(ps: java.sql.PreparedStatement, params: List<Any>) {
        for ((i, p) in params.withIndex()) ps.setObject(i + 1, p)
    }

    private fun mapDoctor(rs: ResultSet): Doctor =
        Doctor(
            id = rs.getString("id"),
            userId = rs.getString("user_id"),
            fullName = rs.getString("full_name"),
            specialty = rs.getString("specialty"),
            specializationId = rs.getString("specialization_id"),
            licenseNumber = rs.getString("license_number"),
            yearsOfExperience = rs.getInt("years_of_experience"),
            feeCentavos = rs.getLong("fee_centavos"),
            bio = rs.getString("bio"),
            location = ClinicLocation(rs.getString("building"), rs.getString("floor"), rs.getString("room")),
            languages = fromJson<List<ConsultationLanguage>>(rs.getString("languages")) ?: emptyList(),
            status = DoctorStatus.valueOf(rs.getString("status")),
            createdAt = fromTimestamp(rs.getTimestamp("created_at"))!!,
            updatedAt = fromTimestamp(rs.getTimestamp("updated_at"))!!,
        )
}
