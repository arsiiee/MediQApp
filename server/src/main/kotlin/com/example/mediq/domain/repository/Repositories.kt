package com.example.mediq.domain.repository

import com.example.mediq.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface AppointmentRepository {
    suspend fun findById(id: String): LoadState<Appointment>
    suspend fun findAll(filter: AppointmentFilter): LoadState<Paged<Appointment>>
    suspend fun create(appointment: Appointment): LoadState<Appointment>
    suspend fun update(appointment: Appointment): LoadState<Appointment>
    suspend fun updateStatus(id: String, status: AppointmentStatus, outcome: VisitOutcome? = null): LoadState<Appointment>
    suspend fun delete(id: String): LoadState<Unit>
    suspend fun findConflicts(doctorId: String, startsAt: kotlinx.datetime.LocalDateTime, durationMinutes: Int, excludeId: String? = null): LoadState<List<Appointment>>
}

interface AuthRepository {
    suspend fun signIn(username: String, password: String): LoadState<AuthSession>
    suspend fun register(request: RegisterRequest): LoadState<AuthSession>
    suspend fun refreshToken(refreshToken: String): LoadState<AuthSession>
    suspend fun revokeToken(refreshToken: String): LoadState<Unit>
    suspend fun storeRefreshToken(token: String, userId: String, expiresAt: kotlinx.datetime.LocalDateTime)
    suspend fun findByUsername(username: String): LoadState<UserProfile?>
    suspend fun findById(id: String): LoadState<UserProfile?>
    suspend fun findByContact(contact: String): LoadState<UserProfile?>
    suspend fun updateProfile(profile: UserProfile): LoadState<UserProfile>
    suspend fun updatePassword(userId: String, newPasswordHash: String): LoadState<Unit>

    /** Keeps a one-time code until it is used or expires. */
    suspend fun saveOtp(key: String, code: String, expiresAt: kotlinx.datetime.LocalDateTime)

    /** The code kept under [key], or null when there is none. */
    suspend fun findOtp(key: String): OtpEntry?

    /** Forgets the code kept under [key]. */
    suspend fun deleteOtp(key: String)
}

interface DoctorRepository {
    suspend fun findById(id: String): LoadState<Doctor>
    suspend fun findByUserId(userId: String): LoadState<Doctor?>
    suspend fun findAll(query: DoctorQuery): LoadState<Paged<Doctor>>
    suspend fun create(doctor: Doctor): LoadState<Doctor>
    suspend fun update(doctor: Doctor): LoadState<Doctor>
    suspend fun updateStatus(id: String, status: DoctorStatus): LoadState<Doctor>
    suspend fun findBySpecialization(specializationId: String): LoadState<List<Doctor>>
    suspend fun getSchedule(doctorId: String): LoadState<List<ClinicHours>>
    suspend fun saveSchedule(doctorId: String, schedule: List<ClinicHours>): LoadState<Unit>
    suspend fun getAvailability(doctorId: String, date: kotlinx.datetime.LocalDate): LoadState<List<AvailableDate>>
    suspend fun saveAvailability(doctorId: String, availability: AvailableDate): LoadState<Unit>
}

interface MessageRepository {
    suspend fun findConversation(id: String): LoadState<MessageThread?>
    suspend fun findConversationsForDoctor(doctorId: String): LoadState<List<MessageThread>>
    suspend fun findConversationsForPatient(patientId: String): LoadState<List<MessageThread>>
    suspend fun findConversationsForSecretary(secretaryId: String): LoadState<List<MessageThread>>
    suspend fun findMessages(conversationId: String): LoadState<List<Message>>
    suspend fun createMessage(message: Message): LoadState<Message>
    suspend fun markAsRead(conversationId: String, userId: String): LoadState<Unit>
    suspend fun createConversation(thread: MessageThread): LoadState<MessageThread>
}

interface NotificationRepository {
    suspend fun findByUserId(userId: String, unreadOnly: Boolean = false): LoadState<List<Notification>>
    suspend fun create(notification: Notification): LoadState<Notification>
    suspend fun markAsRead(id: String): LoadState<Unit>
    suspend fun markAllAsRead(userId: String): LoadState<Unit>
}

interface ProfileRepository {
    suspend fun findById(id: String): LoadState<UserProfile>
    suspend fun findByUsername(username: String): LoadState<UserProfile?>
    suspend fun update(profile: UserProfile): LoadState<UserProfile>
    suspend fun updateRole(userId: String, role: UserRole): LoadState<UserProfile>
    suspend fun updateStatus(userId: String, status: AccountStatus): LoadState<UserProfile>
}

interface SpecializationRepository {
    suspend fun findAll(): LoadState<List<Specialty>>
    suspend fun findById(id: String): LoadState<Specialty>
    suspend fun create(specialty: Specialty): LoadState<Specialty>
    suspend fun update(specialty: Specialty): LoadState<Specialty>
    suspend fun delete(id: String): LoadState<Unit>
}

interface AdminRepository {
    suspend fun getAllAccounts(): LoadState<AdminAccountsResponse>
    suspend fun createStaffAccount(draft: RegistrationDraft): LoadState<UserProfile>
    suspend fun updateAccountStatus(userId: String, status: AccountStatus): LoadState<UserProfile>
    suspend fun getDoctorsForAdmin(): LoadState<List<Doctor>>
}

interface AuditRepository {
    suspend fun findAll(): LoadState<List<AuditEntry>>
    suspend fun create(entry: AuditEntry): LoadState<AuditEntry>
}

@Serializable
data class AdminAccountsResponse(
    val patients: List<UserProfile>,
    val doctors: List<UserProfile>,
    val secretaries: List<UserProfile>,
)