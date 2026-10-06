package com.example.mediq.data.api

/**
 * Wire DTOs that mirror the server's http/Dtos.kt.
 *
 * All date, time, and enum fields are represented as [String] so that Gson
 * deserialises them verbatim without needing custom type adapters for
 * java.time.*. Conversion to domain types is done in ApiMappers.kt.
 *
 * These classes must never leak into the domain layer or the UI — they exist
 * solely as an intermediate between JSON and the domain models.
 */

// ─── Nested value types ─────────────────────────────────────────────────────

data class MoneyDto(val amountInCentavos: Long)

data class ClinicLocationDto(
    val building: String,
    val floor: String,
    val room: String,
)

data class ClinicHoursDto(
    val dayOfWeek: Int,    // 1 = Monday … 7 = Sunday
    val opensAt: String,   // "09:00"
    val closesAt: String,  // "17:00"
)

// ─── Resource DTOs ───────────────────────────────────────────────────────────

data class DoctorDto(
    val id: String,
    val fullName: String,
    val specialty: String,
    val yearsOfExperience: Int,
    val consultationFee: MoneyDto,
    val location: ClinicLocationDto,
    val licenseNumber: String,
    val bio: String,
    val languages: List<String>,
    val clinicHours: List<ClinicHoursDto>,
)

data class TimeSlotDto(
    val id: String,
    val doctorId: String,
    val startsAt: String,   // ISO-8601 UTC e.g. "2026-10-05T01:30:00Z"
    val endsAt: String,
    val status: String,     // "available" | "reserved" | "blocked"
)

data class AvailableDateDto(
    val date: String,        // "YYYY-MM-DD"
    val openSlotCount: Int,
)

data class AppointmentDto(
    val id: String,
    val doctor: DoctorDto,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val location: ClinicLocationDto,
    val fee: MoneyDto,
    val reasonForVisit: String? = null,
)

data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val readAt: String? = null,
    val relatedAppointmentId: String? = null,
)

data class UserProfileDto(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String?,
    val mobileNumber: String,
    val dateOfBirth: String,   // "YYYY-MM-DD"
    val sex: String?,
    val address: String?,
    val role: String,
)

data class AuthSessionDto(
    val accessToken: String,
    val expiresAt: String,
    val profile: UserProfileDto,
)

data class PagedDto<T>(
    val items: List<T>,
    val nextCursor: String? = null,
)

data class OtpRequestedDto(
    val sent: Boolean,
    val code: String? = null,   // non-null in dev mode (returnCodeToCaller = true)
)

/** Generic acknowledgement for endpoints that return {"ok": true}. */
data class AckDto(val ok: Boolean)

/**
 * The error body every non-2xx response carries.
 *
 * Mirrors `ErrorDto` in `server/.../http/Dtos.kt`. The server commits to this
 * shape from one place — `configureStatusPages` in `Routes.kt` — so a client
 * that can parse it can rely on it existing.
 *
 * Both fields are nullable because Gson happily produces null for absent
 * properties on a non-null Kotlin type, and a body that is not this shape at
 * all (an HTML error page from a proxy, an empty body) must not throw here.
 */
data class ErrorDto(
    val error: String? = null,
    val message: String? = null,
)

// ─── Request DTOs ────────────────────────────────────────────────────────────

data class SignInRequestDto(val username: String, val password: String)

data class RequestOtpRequestDto(val mobileNumber: String)

data class VerifyOtpRequestDto(val mobileNumber: String, val otp: String)

data class RegisterRequestDto(
    val registrationId: String,
    val fullName: String,
    val username: String,
    val password: String,
    val dateOfBirth: String?,
    val sex: String?,
    val email: String?,
)

data class BookingRequestDto(
    val slotId: String,
    val reasonForVisit: String?,
    val confirmedByPatient: Boolean,
)

data class RescheduleRequestDto(
    val requestedSlotId: String,
    val note: String? = null,
)

data class UpdateProfileDto(
    val fullName: String? = null,
    val email: String? = null,
    val mobileNumber: String? = null,
    val dateOfBirth: String? = null,
    val sex: String? = null,
    val address: String? = null,
)
