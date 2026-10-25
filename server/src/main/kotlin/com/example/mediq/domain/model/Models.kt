package com.example.mediq.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable

data class ApiFailure(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
) {
    companion object {
        fun validation(message: String, details: Map<String, String>? = null) = ApiFailure("VALIDATION_ERROR", message, details)
        fun notFound(message: String) = ApiFailure("NOT_FOUND", message)
        fun unauthorized(message: String) = ApiFailure("UNAUTHORIZED", message)
        fun forbidden(message: String) = ApiFailure("FORBIDDEN", message)
        fun conflict(message: String) = ApiFailure("CONFLICT", message)
        fun internal(message: String) = ApiFailure("INTERNAL_ERROR", message)
    }
}

@Serializable

sealed class LoadState<out T> {
    @Serializable data class Loading<T>(val progress: Float = 0f) : LoadState<T>()
    @Serializable data class Success<T>(val data: T) : LoadState<T>()
    @Serializable data class Error(val failure: ApiFailure) : LoadState<Nothing>()

    suspend fun <R> fold(onSuccess: suspend (T) -> R, onError: suspend (ApiFailure) -> R): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(failure)
        is Loading -> onError(ApiFailure.internal("Still loading"))
    }

    /** The value when this is a success, or null. */
    fun getOrNull(): T? = (this as? Success)?.data
}

@Serializable
data class Appointment(
    val id: String,
    val patientId: String,
    val doctorId: String,
    val startsAt: LocalDateTime,
    val durationMinutes: Int,
    val status: AppointmentStatus,
    val reason: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val outcome: VisitOutcome? = null,
)

@Serializable
enum class AppointmentStatus(val serialName: String) {
    @SerialName("booked") BOOKED("booked"),
    @SerialName("confirmed") CONFIRMED("confirmed"),
    @SerialName("completed") COMPLETED("completed"),
    @SerialName("cancelled") CANCELLED("cancelled"),
    @SerialName("no_show") NO_SHOW("no_show"),
}

@Serializable
enum class VisitOutcome {
    @SerialName("completed") COMPLETED,
    @SerialName("no_show") NO_SHOW,
}

@Serializable
data class ScheduleBlockWire(
    val startTime: String,
    val endTime: String,
)

@Serializable
data class ScheduleDayWire(
    val dayOfWeek: Int,
    val sessions: List<ScheduleBlockWire> = emptyList(),
)

@Serializable
data class DatedAvailabilityWire(
    val date: String,
    val sessions: List<ScheduleBlockWire> = emptyList(),
)

@Serializable
data class AccountStatusUpdate(
    val status: AccountStatus,
)

@Serializable

data class AppointmentFilter(
    val doctorId: String? = null,
    val patientId: String? = null,
    val status: AppointmentStatus? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val limit: Int = 50,
    val offset: Int = 0,
)

@Serializable

data class AuthSession(
    val accessToken: String,
    val expiresAt: LocalDateTime,
    val refreshToken: String,
    val refreshExpiresAt: LocalDateTime,
    val profile: UserProfile,
)

@Serializable

data class AvailableDate(
    val date: LocalDate,
    val sessions: List<TimeSlot>,
    val isOverride: Boolean,
)

@Serializable

data class BookingRequest(
    val patientId: String,
    val doctorId: String,
    val startsAt: LocalDateTime,
    val durationMinutes: Int,
    val reason: String,
)

@Serializable

data class ClinicHours(
    val dayOfWeek: Int, // 0 = Sunday
    val startTime: String, // HH:MM
    val endTime: String, // HH:MM
    val isClosed: Boolean = false,
)

@Serializable

data class ClinicLocation(
    val building: String,
    val floor: String,
    val room: String,
)

@Serializable

data class ConsultationLanguage(
    val code: String, // ISO 639-1
    val name: String,
)

@Serializable

data class Doctor(
    val id: String,
    val userId: String,
    val fullName: String,
    val specialty: String,
    val specializationId: String,
    val licenseNumber: String,
    val yearsOfExperience: Int,
    val feeCentavos: Long,
    val bio: String,
    val location: ClinicLocation,
    val languages: List<ConsultationLanguage>,
    val status: DoctorStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@Serializable
enum class DoctorStatus {
    @SerialName("active") ACTIVE,
    @SerialName("inactive") INACTIVE,
    @SerialName("on_leave") ON_LEAVE,
}

@Serializable

data class DoctorQuery(
    val specializationId: String? = null,
    val search: String? = null,
    val status: DoctorStatus? = null,
    val limit: Int = 20,
    val offset: Int = 0,
)

@Serializable

data class Message(
    val id: String,
    val conversationId: String,
    val authorId: String,
    val authorRole: MessageAuthorRole,
    val body: String,
    val sentAt: LocalDateTime,
    val readAt: LocalDateTime? = null,
)

@Serializable
enum class MessageAuthorRole(val serialName: String) {
    @SerialName("patient") PATIENT("patient"),
    @SerialName("doctor") DOCTOR("doctor"),
    @SerialName("secretary") SECRETARY("secretary"),
    @SerialName("admin") ADMIN("admin"),
}

@Serializable

data class MessageThread(
    val id: String,
    val patientId: String,
    val doctorId: String,
    val subject: String,
    val lastMessageAt: LocalDateTime,
    val unreadCount: Int,
    val awaitingAction: Boolean,
)

@Serializable

data class Money(
    val centavos: Long,
    val currency: String = "PHP",
) {
    companion object {
        fun fromCentavos(centavos: Long) = Money(centavos)
        fun fromPesos(pesos: Double) = Money(round(pesos * 100).toLong())
    }
}

@Serializable

data class Notification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, String>? = null,
    val isRead: Boolean = false,
    val createdAt: LocalDateTime,
    val readAt: LocalDateTime? = null,
)

@Serializable
enum class NotificationType {
    @SerialName("appointment_booked") APPOINTMENT_BOOKED,
    @SerialName("appointment_confirmed") APPOINTMENT_CONFIRMED,
    @SerialName("appointment_cancelled") APPOINTMENT_CANCELLED,
    @SerialName("appointment_reminder") APPOINTMENT_REMINDER,
    @SerialName("message_received") MESSAGE_RECEIVED,
    @SerialName("system") SYSTEM,
}

@Serializable

data class Paged<T>(
    val items: List<T>,
    val total: Long,
    val limit: Int,
    val offset: Int,
) {
    val hasMore: Boolean get() = offset + items.size < total
}

@Serializable

data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val email: String?,
    val mobileNumber: String,
    val dateOfBirth: LocalDate,
    val role: UserRole,
)

@Serializable

data class RegistrationDraft(
    val username: String,
    val password: String,
    val fullName: String,
    val email: String?,
    val mobileNumber: String,
    val dateOfBirth: LocalDate,
    val role: UserRole,
    val practice: DoctorPractice? = null,
    val assignedDoctorId: String? = null,
)

@Serializable

data class RequestOtpRequest(
    val contact: String, // email or phone
    val purpose: OtpPurpose,
)

/** A one-time code and the moment it stops working. */
@Serializable
data class OtpEntry(
    val code: String,
    val expiresAt: LocalDateTime
)

/** The new password a patient chose after proving they own the number. */
@Serializable
data class UpdatePasswordRequest(
    val newPassword: String
)

@Serializable
enum class OtpPurpose {
    @SerialName("register") REGISTER,
    @SerialName("reset_password") RESET_PASSWORD,
}

@Serializable

data class RescheduleRequest(
    val startsAt: LocalDateTime,
    val durationMinutes: Int,
)

@Serializable
enum class SessionSignal {
    @SerialName("sign_in") SIGN_IN,
    @SerialName("sign_out") SIGN_OUT,
    @SerialName("token_refresh") TOKEN_REFRESH,
}

@Serializable
enum class Sex {
    @SerialName("male") MALE,
    @SerialName("female") FEMALE,
    @SerialName("other") OTHER,
}

@Serializable

data class SignInRequest(
    val username: String,
    val password: String,
)

@Serializable
enum class SlotStatus {
    @SerialName("available") AVAILABLE,
    @SerialName("booked") BOOKED,
    @SerialName("blocked") BLOCKED,
}

@Serializable

data class Specialty(
    val id: String,
    val name: String,
    val description: String?,
    val doctorCount: Int = 0,
)

@Serializable

data class TimeSlot(
    val startTime: String, // HH:MM
    val endTime: String, // HH:MM
    val status: SlotStatus = SlotStatus.AVAILABLE,
    val appointmentId: String? = null,
)

@Serializable

data class UserProfile(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String?,
    val mobileNumber: String,
    val dateOfBirth: LocalDate,
    val role: UserRole,
    val status: AccountStatus,
    val doctorId: String? = null,
    val assignedDoctorId: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@Serializable
data class AuditEntry(
    val id: String,
    val actor: String,
    val action: String,
    val target: String,
    val at: LocalDateTime,
    val severity: AuditSeverity,
)

@Serializable
enum class AuditSeverity(val serialName: String) {
    @SerialName("info") INFO("info"),
    @SerialName("warning") WARNING("warning"),
    @SerialName("error") ERROR("error"),
}

@Serializable
enum class UserRole(val serialName: String) {
    @SerialName("patient") PATIENT("patient"),
    @SerialName("doctor") DOCTOR("doctor"),
    @SerialName("secretary") SECRETARY("secretary"),
    @SerialName("admin") ADMIN("admin"),
}

@Serializable
enum class AccountStatus(val serialName: String) {
    @SerialName("active") ACTIVE("active"),
    @SerialName("inactive") INACTIVE("inactive"),
    @SerialName("pending_verification") PENDING_VERIFICATION("pending_verification"),
    @SerialName("suspended") SUSPENDED("suspended"),
}

@Serializable

data class VerifyOtpRequest(
    val contact: String,
    val code: String,
    val purpose: OtpPurpose,
)

@Serializable

data class DoctorPractice(
    val specializationId: String,
    val licenseNumber: String,
    val yearsExperience: Int,
    val feeCentavos: Long,
    val bio: String,
    val building: String,
    val floor: String,
    val room: String,
    val languages: List<ConsultationLanguage>,
)
