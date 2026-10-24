package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class JdbcProfileRepository(dataSource: HikariDataSource) : JdbcRepositoryBase(dataSource), ProfileRepository {

    override suspend fun findById(id: String): LoadState<UserProfile> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM users WHERE id = ?").use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                if (rs.next()) LoadState.Success(mapUser(rs))
                else LoadState.Error(ApiFailure.notFound("Profile not found"))
            }
        }
    }

    override suspend fun findByUsername(username: String): LoadState<UserProfile?> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM users WHERE username = ?").use { ps ->
            ps.setString(1, username)
            ps.executeQuery().use { rs ->
                LoadState.Success(if (rs.next()) mapUser(rs) else null)
            }
        }
    }

    override suspend fun update(profile: UserProfile): LoadState<UserProfile> = withConnection { conn ->
        conn.prepareStatement("UPDATE users SET username = ?, full_name = ?, email = ?, mobile_number = ?, date_of_birth = ?, role = ?, status = ?, doctor_id = ?, assigned_doctor_id = ?, updated_at = ? WHERE id = ?").use { ps ->
            ps.setString(1, profile.username)
            ps.setString(2, profile.fullName)
            ps.setString(3, profile.email)
            ps.setString(4, profile.mobileNumber)
            ps.setString(5, toDateString(profile.dateOfBirth))
            ps.setString(6, profile.role.name)
            ps.setString(7, profile.status.name)
            ps.setString(8, profile.doctorId)
            ps.setString(9, profile.assignedDoctorId)
            ps.setTimestamp(10, toTimestamp(nowUtc()))
            ps.setString(11, profile.id)
            val updated = ps.executeUpdate()
            if (updated == 0) LoadState.Error(ApiFailure.notFound("Profile not found"))
            else LoadState.Success(profile.copy(updatedAt = nowUtc()))
        }
    }

    override suspend fun updateRole(userId: String, role: UserRole): LoadState<UserProfile> = withConnection { conn ->
        conn.prepareStatement("UPDATE users SET role = ?, updated_at = ? WHERE id = ?").use { ps ->
            ps.setString(1, role.name)
            ps.setTimestamp(2, toTimestamp(nowUtc()))
            ps.setString(3, userId)
            val updated = ps.executeUpdate()
            if (updated == 0) LoadState.Error(ApiFailure.notFound("Profile not found"))
            else findById(userId)
        }
    }

    override suspend fun updateStatus(userId: String, status: AccountStatus): LoadState<UserProfile> = withConnection { conn ->
        conn.prepareStatement("UPDATE users SET status = ?, updated_at = ? WHERE id = ?").use { ps ->
            ps.setString(1, status.name)
            ps.setTimestamp(2, toTimestamp(nowUtc()))
            ps.setString(3, userId)
            val updated = ps.executeUpdate()
            if (updated == 0) LoadState.Error(ApiFailure.notFound("Profile not found"))
            else findById(userId)
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
}
