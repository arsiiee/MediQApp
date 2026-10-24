package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.example.mediq.security.PasswordHash
import com.example.mediq.util.plusDays
import com.example.mediq.util.plusMinutes
import com.zaxxer.hikari.HikariDataSource
import kotlinx.datetime.LocalDateTime
import java.sql.ResultSet

class JdbcAuthRepository(
    dataSource: HikariDataSource,
    private val passwordHash: PasswordHash,
) : JdbcRepositoryBase(dataSource), AuthRepository {

    override suspend fun signIn(username: String, password: String): LoadState<AuthSession> = withConnection { conn ->
        conn.prepareStatement("SELECT u.*, ph.hash FROM users u LEFT JOIN password_hashes ph ON u.id = ph.user_id WHERE u.username = ?").use { ps ->
            ps.setString(1, username)
            ps.executeQuery().use { rs ->
                if (!rs.next()) {
                    LoadState.Error(ApiFailure.unauthorized("Invalid credentials"))
                } else {
                    val user = mapUser(rs)
                    val storedHash = rs.getString("hash")
                    if (storedHash == null || !passwordHash.verify(password, storedHash)) {
                        LoadState.Error(ApiFailure.unauthorized("Invalid credentials"))
                    } else {
                        val now = nowUtc()
                        LoadState.Success(AuthSession(
                            accessToken = "",
                            expiresAt = now.plusMinutes(15),
                            refreshToken = "",
                            refreshExpiresAt = now.plusDays(30),
                            profile = user,
                        ))
                    }
                }
            }
        }
    }

    override suspend fun register(request: RegisterRequest): LoadState<AuthSession> = withConnection { conn ->
        conn.autoCommit = false
        try {
            val exists = conn.prepareStatement("SELECT id FROM users WHERE username = ?").use { ps ->
                ps.setString(1, request.username)
                ps.executeQuery().use { rs -> rs.next() }
            }
            if (exists) {
                conn.rollback()
                return@withConnection LoadState.Error(ApiFailure.conflict("Username already exists"))
            }
            val id = newId()
            val now = nowUtc()
            val hash = passwordHash.hash(request.password)
            conn.prepareStatement("INSERT INTO users (id, username, full_name, email, mobile_number, date_of_birth, role, status, doctor_id, assigned_doctor_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { ps ->
                ps.setString(1, id)
                ps.setString(2, request.username)
                ps.setString(3, request.fullName)
                ps.setString(4, request.email)
                ps.setString(5, request.mobileNumber)
                ps.setString(6, toDateString(request.dateOfBirth))
                ps.setString(7, request.role.name)
                ps.setString(8, AccountStatus.ACTIVE.name)
                ps.setString(9, null)
                ps.setString(10, null)
                ps.setTimestamp(11, toTimestamp(now))
                ps.setTimestamp(12, toTimestamp(now))
                ps.executeUpdate()
            }
            conn.prepareStatement("INSERT INTO password_hashes (user_id, hash) VALUES (?, ?)").use { ps ->
                ps.setString(1, id)
                ps.setString(2, hash)
                ps.executeUpdate()
            }
            conn.commit()
            val profile = UserProfile(
                id = id, username = request.username, fullName = request.fullName, email = request.email,
                mobileNumber = request.mobileNumber, dateOfBirth = request.dateOfBirth, role = request.role,
                status = AccountStatus.ACTIVE, createdAt = now, updatedAt = now,
            )
            LoadState.Success(AuthSession(
                accessToken = "",
                expiresAt = now.plusMinutes(15),
                refreshToken = "",
                refreshExpiresAt = now.plusDays(30),
                profile = profile,
            ))
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }

    override suspend fun refreshToken(refreshToken: String): LoadState<AuthSession> = withConnection { conn ->
        conn.prepareStatement("SELECT user_id FROM refresh_tokens WHERE token = ? AND expires_at > ?").use { ps ->
            ps.setString(1, refreshToken)
            ps.setTimestamp(2, toTimestamp(nowUtc()))
            ps.executeQuery().use { rs ->
                if (!rs.next()) {
                    LoadState.Error(ApiFailure.unauthorized("Invalid or expired refresh token"))
                } else {
                    val userId = rs.getString("user_id")
                    findById(userId).getOrNull()?.let { user ->
                        val now = nowUtc()
                        LoadState.Success(AuthSession(
                            accessToken = "",
                            expiresAt = now.plusMinutes(15),
                            refreshToken = "",
                            refreshExpiresAt = now.plusDays(30),
                            profile = user,
                        ))
                    } ?: LoadState.Error(ApiFailure.unauthorized("User not found"))
                }
            }
        }
    }

    override suspend fun revokeToken(refreshToken: String): LoadState<Unit> = withConnection { conn ->
        conn.prepareStatement("DELETE FROM refresh_tokens WHERE token = ?").use { ps ->
            ps.setString(1, refreshToken)
            ps.executeUpdate()
        }
        LoadState.Success(Unit)
    }

    override suspend fun storeRefreshToken(token: String, userId: String, expiresAt: LocalDateTime) = withConnection { conn ->
        conn.prepareStatement("INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)").use { ps ->
            ps.setString(1, token)
            ps.setString(2, userId)
            ps.setTimestamp(3, toTimestamp(expiresAt))
            ps.executeUpdate()
        }
        Unit
    }

    override suspend fun findByUsername(username: String): LoadState<UserProfile?> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM users WHERE username = ?").use { ps ->
            ps.setString(1, username)
            ps.executeQuery().use { rs ->
                LoadState.Success(if (rs.next()) mapUser(rs) else null)
            }
        }
    }

    override suspend fun findById(id: String): LoadState<UserProfile?> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM users WHERE id = ?").use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                LoadState.Success(if (rs.next()) mapUser(rs) else null)
            }
        }
    }

    override suspend fun findByContact(contact: String): LoadState<UserProfile?> = withConnection { conn ->
        val wanted = contact.trim()
        conn.prepareStatement("SELECT * FROM users WHERE mobile_number = ? OR email = ?").use { ps ->
            ps.setString(1, wanted)
            ps.setString(2, wanted)
            ps.executeQuery().use { rs ->
                LoadState.Success(if (rs.next()) mapUser(rs) else null)
            }
        }
    }

    override suspend fun updateProfile(profile: UserProfile): LoadState<UserProfile> = withConnection { conn ->
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

    override suspend fun updatePassword(userId: String, newPasswordHash: String): LoadState<Unit> = withConnection { conn ->
        conn.prepareStatement("UPDATE password_hashes SET hash = ? WHERE user_id = ?").use { ps ->
            ps.setString(1, newPasswordHash)
            ps.setString(2, userId)
            ps.executeUpdate()
        }
        LoadState.Success(Unit)
    }

    override suspend fun saveOtp(key: String, code: String, expiresAt: LocalDateTime) = withConnection { conn ->
        conn.prepareStatement("DELETE FROM otp_codes WHERE otp_key = ?").use { ps ->
            ps.setString(1, key)
            ps.executeUpdate()
        }
        conn.prepareStatement("INSERT INTO otp_codes (otp_key, code, expires_at) VALUES (?, ?, ?)").use { ps ->
            ps.setString(1, key)
            ps.setString(2, code)
            ps.setTimestamp(3, toTimestamp(expiresAt))
            ps.executeUpdate()
        }
        Unit
    }

    override suspend fun findOtp(key: String): OtpEntry? = withConnection { conn ->
        conn.prepareStatement("SELECT code, expires_at FROM otp_codes WHERE otp_key = ?").use { ps ->
            ps.setString(1, key)
            ps.executeQuery().use { rs ->
                if (rs.next()) OtpEntry(rs.getString("code"), fromTimestamp(rs.getTimestamp("expires_at"))!!)
                else null
            }
        }
    }

    override suspend fun deleteOtp(key: String) = withConnection { conn ->
        conn.prepareStatement("DELETE FROM otp_codes WHERE otp_key = ?").use { ps ->
            ps.setString(1, key)
            ps.executeUpdate()
        }
        Unit
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
