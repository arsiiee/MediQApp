package com.example.mediq.server

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.example.mediq.security.PasswordHash
import com.example.mediq.util.isBefore
import com.example.mediq.util.plusDays
import com.example.mediq.util.plusMinutes
import com.example.mediq.util.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class InMemoryAuthRepository(val passwordHash: PasswordHash) : AuthRepository {
    internal val users = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    internal val passwordHashes = MutableStateFlow<Map<String, String>>(emptyMap())
    private val refreshTokens = MutableStateFlow<Map<String, Pair<String, LocalDateTime>>>(emptyMap()) // token -> (userId, expiresAt)

    /** One-time codes, keyed by contact and purpose. */
    private val otpCodes = MutableStateFlow<Map<String, OtpEntry>>(emptyMap())

    /** Password assigned to the seeded demo accounts. */
    private val demoPassword = "demo12345"

    init {
        // Seed demo users
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val demoUsers = listOf(
            UserProfile("doc-1", "dr.smith", "Dr. Sarah Smith", "dr.smith@mediq.app", "+639171234567", LocalDate.parse("1980-05-15"), UserRole.DOCTOR, AccountStatus.ACTIVE, doctorId = "doc-1", createdAt = now, updatedAt = now),
            UserProfile("doc-2", "dr.chen", "Dr. Michael Chen", "dr.chen@mediq.app", "+639172345678", LocalDate.parse("1975-11-22"), UserRole.DOCTOR, AccountStatus.ACTIVE, doctorId = "doc-2", createdAt = now, updatedAt = now),
            UserProfile("sec-1", "sec.jones", "Secretary Jane Jones", "sec.jones@mediq.app", "+639173456789", LocalDate.parse("1990-03-10"), UserRole.SECRETARY, AccountStatus.ACTIVE, assignedDoctorId = "doc-1", createdAt = now, updatedAt = now),
            UserProfile("admin-1", "admin", "Admin User", "admin@mediq.app", "+639170000000", LocalDate.parse("1985-01-01"), UserRole.ADMIN, AccountStatus.ACTIVE, createdAt = now, updatedAt = now),
            UserProfile("pat-1", "demo_patient", "Demo Patient", "demo@patient.com", "+639181111111", LocalDate.parse("1995-07-20"), UserRole.PATIENT, AccountStatus.ACTIVE, createdAt = now, updatedAt = now),
            UserProfile("pat-2", "demo_patient2", "Maria Santos", "maria@patient.com", "+639182222222", LocalDate.parse("1988-12-05"), UserRole.PATIENT, AccountStatus.ACTIVE, createdAt = now, updatedAt = now),
        )
        users.value = demoUsers.associateBy { it.id }
        val demoHash = passwordHash.hash(demoPassword)
        passwordHashes.value = demoUsers.associate { it.id to demoHash }

        println("[MediQ] Demo patient: demo_patient / $demoPassword")
    }

    override suspend fun signIn(username: String, password: String): LoadState<AuthSession> {
        val user = users.value.values.find { it.username == username }
        if (user == null) {
            return LoadState.Error(ApiFailure.unauthorized("Invalid credentials"))
        }
        val storedHash = passwordHashes.value[user.id]
        if (storedHash == null || !passwordHash.verify(password, storedHash)) {
            return LoadState.Error(ApiFailure.unauthorized("Invalid credentials"))
        }
        return LoadState.Success(AuthSession(
            accessToken = "",
            expiresAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).plusMinutes(15),
            refreshToken = "",
            refreshExpiresAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).plusDays(30),
            profile = user,
        ))
    }

    override suspend fun register(request: RegisterRequest): LoadState<AuthSession> {
        if (users.value.values.any { it.username == request.username }) {
            return LoadState.Error(ApiFailure.conflict("Username already exists"))
        }
        val passwordHash = this.passwordHash.hash(request.password)
        val profile = UserProfile(
            id = java.util.UUID.randomUUID().toString(),
            username = request.username,
            fullName = request.fullName,
            email = request.email,
            mobileNumber = request.mobileNumber ?: "",
            dateOfBirth = request.dateOfBirth,
            role = request.role,
            status = AccountStatus.ACTIVE,
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
        )
        users.update { it + (profile.id to profile) }
        passwordHashes.update { it + (profile.id to passwordHash) }
        return LoadState.Success(AuthSession(
            accessToken = "",
            expiresAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).plusMinutes(15),
            refreshToken = "",
            refreshExpiresAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).plusDays(30),
            profile = profile,
        ))
    }

    override suspend fun findByContact(contact: String): LoadState<UserProfile?> {
        val wanted = contact.trim()
        return LoadState.Success(
            users.value.values.firstOrNull {
                it.mobileNumber.equals(wanted, ignoreCase = true) ||
                    it.email?.equals(wanted, ignoreCase = true) == true
            }
        )
    }

    override suspend fun saveOtp(key: String, code: String, expiresAt: kotlinx.datetime.LocalDateTime) {
        otpCodes.update { it + (key to OtpEntry(code, expiresAt)) }
    }

    override suspend fun findOtp(key: String): OtpEntry? = otpCodes.value[key]

    override suspend fun deleteOtp(key: String) {
        otpCodes.update { it - key }
    }

    override suspend fun refreshToken(refreshToken: String): LoadState<AuthSession> {
        val stored = refreshTokens.value[refreshToken]
        if (stored == null || stored.second.isBefore(Clock.System.now().toLocalDateTime(TimeZone.UTC))) {
            return LoadState.Error(ApiFailure.unauthorized("Invalid or expired refresh token"))
        }
        val user = users.value[stored.first]
        return user?.let { LoadState.Success(AuthSession("", Clock.System.now().toLocalDateTime(TimeZone.UTC).plusMinutes(15), "", Clock.System.now().toLocalDateTime(TimeZone.UTC).plusDays(30), it)) }
            ?: LoadState.Error(ApiFailure.unauthorized("User not found"))
    }

    override suspend fun revokeToken(refreshToken: String): LoadState<Unit> {
        refreshTokens.update { it - refreshToken }
        return LoadState.Success(Unit)
    }

    override suspend fun storeRefreshToken(token: String, userId: String, expiresAt: kotlinx.datetime.LocalDateTime) {
        refreshTokens.update { it + (token to (userId to expiresAt)) }
    }

    override suspend fun findByUsername(username: String): LoadState<UserProfile?> = LoadState.Success(users.value.values.find { it.username == username })

    override suspend fun findById(id: String): LoadState<UserProfile?> = LoadState.Success(users.value[id])

    override suspend fun updateProfile(profile: UserProfile): LoadState<UserProfile> {
        users.update { it + (profile.id to profile.copy(updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC))) }
        return LoadState.Success(profile)
    }

    override suspend fun updatePassword(userId: String, newPasswordHash: String): LoadState<Unit> {
        passwordHashes.update { it + (userId to newPasswordHash) }
        return LoadState.Success(Unit)
    }
}
