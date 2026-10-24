package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.example.mediq.security.PasswordHash
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class JdbcAdminRepository(
    dataSource: HikariDataSource,
    private val passwordHash: PasswordHash,
) : JdbcRepositoryBase(dataSource), AdminRepository {

    override suspend fun getAllAccounts(): LoadState<AdminAccountsResponse> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM users ORDER BY role, username").use { ps ->
            ps.executeQuery().use { rs ->
                val patients = mutableListOf<UserProfile>()
                val doctors = mutableListOf<UserProfile>()
                val secretaries = mutableListOf<UserProfile>()
                while (rs.next()) {
                    val profile = mapUser(rs)
                    when (profile.role) {
                        UserRole.PATIENT -> patients.add(profile)
                        UserRole.DOCTOR -> doctors.add(profile)
                        UserRole.SECRETARY -> secretaries.add(profile)
                        UserRole.ADMIN -> { }
                    }
                }
                LoadState.Success(AdminAccountsResponse(patients, doctors, secretaries))
            }
        }
    }

    override suspend fun createStaffAccount(draft: RegistrationDraft): LoadState<UserProfile> = withConnection { conn ->
        conn.autoCommit = false
        try {
            val exists = conn.prepareStatement("SELECT id FROM users WHERE username = ?").use { ps ->
                ps.setString(1, draft.username)
                ps.executeQuery().use { rs -> rs.next() }
            }
            if (exists) {
                conn.rollback()
                return@withConnection LoadState.Error(ApiFailure.conflict("Username already exists"))
            }
            val id = newId()
            val now = nowUtc()
            val hash = passwordHash.hash(draft.password)
            conn.prepareStatement("INSERT INTO users (id, username, full_name, email, mobile_number, date_of_birth, role, status, doctor_id, assigned_doctor_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { ps ->
                ps.setString(1, id)
                ps.setString(2, draft.username)
                ps.setString(3, draft.fullName)
                ps.setString(4, draft.email)
                ps.setString(5, draft.mobileNumber)
                ps.setString(6, toDateString(draft.dateOfBirth))
                ps.setString(7, draft.role.name)
                ps.setString(8, AccountStatus.ACTIVE.name)
                ps.setString(9, null)
                ps.setString(10, draft.assignedDoctorId)
                ps.setTimestamp(11, toTimestamp(now))
                ps.setTimestamp(12, toTimestamp(now))
                ps.executeUpdate()
            }
            conn.prepareStatement("INSERT INTO password_hashes (user_id, hash) VALUES (?, ?)").use { ps ->
                ps.setString(1, id)
                ps.setString(2, hash)
                ps.executeUpdate()
            }
            var updatedProfile = UserProfile(
                id = id, username = draft.username, fullName = draft.fullName, email = draft.email,
                mobileNumber = draft.mobileNumber, dateOfBirth = draft.dateOfBirth, role = draft.role,
                status = AccountStatus.ACTIVE, createdAt = now, updatedAt = now,
                assignedDoctorId = draft.assignedDoctorId,
            )
            if (draft.role == UserRole.DOCTOR && draft.practice != null) {
                val practice = draft.practice
                val doctorId = newId()
                conn.prepareStatement("INSERT INTO doctors (id, user_id, full_name, specialty, specialization_id, license_number, years_of_experience, fee_centavos, bio, building, floor, room, languages, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { ps ->
                    ps.setString(1, doctorId)
                    ps.setString(2, id)
                    ps.setString(3, draft.fullName)
                    ps.setString(4, "TBD")
                    ps.setString(5, practice.specializationId)
                    ps.setString(6, practice.licenseNumber)
                    ps.setInt(7, practice.yearsExperience)
                    ps.setLong(8, practice.feeCentavos)
                    ps.setString(9, practice.bio)
                    ps.setString(10, practice.building)
                    ps.setString(11, practice.floor)
                    ps.setString(12, practice.room)
                    ps.setString(13, toJson(practice.languages))
                    ps.setString(14, DoctorStatus.ACTIVE.name)
                    ps.setTimestamp(15, toTimestamp(now))
                    ps.setTimestamp(16, toTimestamp(now))
                    ps.executeUpdate()
                }
                updatedProfile = updatedProfile.copy(doctorId = doctorId)
                conn.prepareStatement("UPDATE users SET doctor_id = ? WHERE id = ?").use { ps ->
                    ps.setString(1, doctorId)
                    ps.setString(2, id)
                    ps.executeUpdate()
                }
            }
            conn.commit()
            LoadState.Success(updatedProfile)
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }

    override suspend fun updateAccountStatus(userId: String, status: AccountStatus): LoadState<UserProfile> = withConnection { conn ->
        conn.prepareStatement("UPDATE users SET status = ?, updated_at = ? WHERE id = ?").use { ps ->
            ps.setString(1, status.name)
            ps.setTimestamp(2, toTimestamp(nowUtc()))
            ps.setString(3, userId)
            val updated = ps.executeUpdate()
            if (updated == 0) LoadState.Error(ApiFailure.notFound("User not found"))
            else findUserById(conn, userId)
        }
    }

    override suspend fun getDoctorsForAdmin(): LoadState<List<Doctor>> = withConnection { conn ->
        conn.prepareStatement("SELECT d.* FROM doctors d JOIN users u ON d.user_id = u.id WHERE u.role = 'DOCTOR' ORDER BY d.full_name LIMIT 1000").use { ps ->
            ps.executeQuery().use { rs ->
                val list = mutableListOf<Doctor>()
                while (rs.next()) list.add(mapDoctor(rs))
                LoadState.Success(list)
            }
        }
    }

    private fun findUserById(conn: java.sql.Connection, id: String): LoadState<UserProfile> =
        conn.prepareStatement("SELECT * FROM users WHERE id = ?").use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                if (rs.next()) LoadState.Success(mapUser(rs))
                else LoadState.Error(ApiFailure.notFound("User not found"))
            }
        }

    private fun mapUser(rs: ResultSet): UserProfile =
        UserProfile(
            id = rs.getString("id"),
            username = rs.getString("username"),
            fullName = rs.getString("full_name"),
            email = rs.getString("email"),
            mobileNumber = rs.getString("mobile_number"),
            dateOfBirth = fromDateString(rs.getString("date_of_birth"))!!,
            role = UserRole.valueOf(rs.getString("role")),
            status = AccountStatus.valueOf(rs.getString("status")),
            doctorId = rs.getString("doctor_id"),
            assignedDoctorId = rs.getString("assigned_doctor_id"),
            createdAt = fromTimestamp(rs.getTimestamp("created_at"))!!,
            updatedAt = fromTimestamp(rs.getTimestamp("updated_at"))!!,
        )

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
