package com.example.mediq.server.http

import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentFilter
import com.example.mediq.domain.model.AppointmentStatus
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.BookingRequest
import com.example.mediq.domain.model.ClinicHours
import com.example.mediq.domain.model.ClinicLocation
import com.example.mediq.domain.model.ConsultationLanguage
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.DoctorQuery
import com.example.mediq.domain.model.Money
import com.example.mediq.domain.model.Notification
import com.example.mediq.domain.model.NotificationType
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.RegisterRequest
import com.example.mediq.domain.model.RequestOtpRequest
import com.example.mediq.domain.model.RescheduleRequest
import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.model.SignInRequest
import com.example.mediq.domain.model.SlotStatus
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.model.UserProfile
import com.example.mediq.domain.model.UserRole
import com.example.mediq.domain.model.VerifyOtpRequest
import com.example.mediq.server.ApiError
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * The JSON shapes that go over the wire.
 *
 * These are separate types from the domain models on purpose. The domain can
 * change shape — replacing a field, splitting a class — without silently
 * breaking the API contract, and the contract is written down here where a
 * client developer can read it in one file.
 *
 * Every enum uses its `wireValue`, never the Kotlin constant name.
 */

@Serializable
data class MoneyDto(val amountInCentavos: Long)

@Serializable
data class ClinicLocationDto(
    val building: String,
    val floor: String,
    val room: String,
)

@Serializable
data class ClinicHoursDto(
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val opensAt: String, // "09:00"
    val closesAt: String, // "12:00"
)

@Serializable
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

@Serializable
data class TimeSlotDto(
    val id: String,
    val doctorId: String,
    val startsAt: String, // ISO-8601 UTC, e.g. "2026-10-05T01:30:00Z"
    val endsAt: String,
    val status: String,
)

@Serializable
data class AvailableDateDto(
    val date: String, // "2026-10-05"
    val openSlotCount: Int,
)

@Serializable
data class AppointmentDto(
    val id: String,
    val doctor: DoctorDto,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val location: ClinicLocationDto,
    val fee: MoneyDto,
    /**
     * Only populated on the single-appointment endpoint. Null on every list
     * response, because a reason for visit is health information and a
     * scrolling list is the wrong place to hand it out.
     */
    val reasonForVisit: String? = null,
)

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val readAt: String? = null,
    val relatedAppointmentId: String? = null,
)

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String?,
    val mobileNumber: String,
    val dateOfBirth: String,
    val sex: String?,
    val address: String?,
    val role: String,
)

@Serializable
data class AuthSessionDto(
    val accessToken: String,
    val expiresAt: String,
    val profile: UserProfileDto,
)

@Serializable
data class PagedDto<T>(
    val items: List<T>,
    val nextCursor: String? = null,
)

// --- Requests ---------------------------------------------------------------

@Serializable
data class SignInRequestDto(
    val username: String,
    val password: String,
)

@Serializable
data class RequestOtpRequestDto(val mobileNumber: String)

/**
 * Response to an OTP request.
 *
 * A named type rather than a `Map<String, Any?>`: kotlinx cannot serialise a map
 * whose values have different types, and `sent` is a Boolean while `code` is a
 * String, so the map version compiled and then threw a 500 at the first request.
 */
@Serializable
data class OtpRequestedDto(
    val sent: Boolean,
    /** Only ever present while the OTP is returned to the caller instead of being texted. */
    val code: String? = null,
)

@Serializable
data class VerifyOtpRequestDto(
    val mobileNumber: String,
    val otp: String,
)

@Serializable
data class RegisterRequestDto(
    val registrationId: String,
    val fullName: String,
    val username: String,
    val password: String,
    val dateOfBirth: String? = null,
    val sex: String? = null,
    val email: String? = null,
)

@Serializable
data class BookingRequestDto(
    val slotId: String,
    val reasonForVisit: String? = null,
    val confirmedByPatient: Boolean,
)

/**
 * Body for a reschedule request.
 *
 * `appointmentId` is not in the contract. It is in the path, and the route
 * copies it from there, so requiring it in the body as well would mean two
 * places name the same appointment and a client that sends the wrong one gets
 * silently ignored.
 */
@Serializable
data class RescheduleRequestDto(
    val requestedSlotId: String,
    val note: String? = null,
)

@Serializable
data class UpdateProfileDto(
    val fullName: String? = null,
    val email: String? = null,
    val mobileNumber: String? = null,
    val dateOfBirth: String? = null,
    val sex: String? = null,
    val address: String? = null,
)

@Serializable
data class ErrorDto(
    val error: String,
    val message: String,
)

// --- Mapping ----------------------------------------------------------------

fun Money.toDto() = MoneyDto(amountInCentavos)

fun ClinicLocation.toDto() = ClinicLocationDto(building, floor, room)

fun ClinicHours.toDto() =
    ClinicHoursDto(dayOfWeek.value, opensAt.toString(), closesAt.toString())

fun Doctor.toDto() = DoctorDto(
    id = id,
    fullName = fullName,
    specialty = specialty.wireValue,
    yearsOfExperience = yearsOfExperience,
    consultationFee = consultationFee.toDto(),
    location = location.toDto(),
    licenseNumber = licenseNumber,
    bio = bio,
    languages = languages.map { it.wireValue },
    clinicHours = clinicHours.map { it.toDto() },
)

fun TimeSlot.toDto() =
    TimeSlotDto(id, doctorId, startsAt.toString(), endsAt.toString(), status.wireValue)

fun AvailableDate.toDto() = AvailableDateDto(date.toString(), openSlotCount)

/** [withReason] is only true for the single-appointment endpoint. */
fun Appointment.toDto(withReason: Boolean = false) = AppointmentDto(
    id = id,
    doctor = doctor.toDto(),
    startsAt = startsAt.toString(),
    endsAt = endsAt.toString(),
    status = status.wireValue,
    location = location.toDto(),
    fee = fee.toDto(),
    reasonForVisit = reasonForVisit.takeIf { withReason },
)

fun Notification.toDto() = NotificationDto(
    id = id,
    type = type.wireValue,
    title = title,
    body = body,
    createdAt = createdAt.toString(),
    readAt = readAt?.toString(),
    relatedAppointmentId = relatedAppointmentId,
)

fun UserProfile.toDto() = UserProfileDto(
    id = id,
    username = username,
    fullName = fullName,
    email = email,
    mobileNumber = mobileNumber,
    dateOfBirth = dateOfBirth.toString(),
    sex = sex?.wireValue,
    address = address,
    role = role.wireValue,
)

fun <T, R> Paged<T>.toDto(map: (T) -> R) = PagedDto(items.map(map), nextCursor)

// Enum lookup by wire value, so a renamed constant does not change the
// contract and an unknown wire value fails loudly instead of silently
// defaulting to the first entry.
/**
 * Resolves a wire value to its enum constant, or rejects it as a bad request.
 *
 * The known values are named in the message. This is not a leak: the enum is
 * the published contract, so a client sending an unknown one has a bug, and
 * telling it what is valid is what gets that fixed. It has to be an [ApiError]
 * rather than `error(...)` — an IllegalStateException reaches the catch-all
 * handler and answers 500, which tells a client the server broke.
 */
private fun <T> parse(
    wire: String,
    field: String,
    values: List<T>,
    wireOf: (T) -> String,
): T = values.firstOrNull { wireOf(it) == wire }
    ?: throw ApiError.badRequest(
        "Unknown $field '$wire'. Valid values: ${values.joinToString(", ") { wireOf(it) }}",
    )

fun String.toSpecialty() = parse(this, "specialty", Specialty.entries) { it.wireValue }
fun String.toSex() = parse(this, "sex", Sex.entries) { it.wireValue }
fun String.toUserRole() = parse(this, "role", UserRole.entries) { it.wireValue }
fun String.toSlotStatus() = parse(this, "slotStatus", SlotStatus.entries) { it.wireValue }
fun String.toAppointmentStatus() = parse(this, "appointmentStatus", AppointmentStatus.entries) { it.wireValue }
fun String.toNotificationType() = parse(this, "notificationType", NotificationType.entries) { it.wireValue }
fun String.toAppointmentFilter() = parse(this, "filter", AppointmentFilter.entries) { it.wireValue }
fun String.toConsultationLanguage() = parse(this, "language", ConsultationLanguage.entries) { it.wireValue }

fun SignInRequestDto.toDomain() = SignInRequest(username, password)

fun RequestOtpRequestDto.toDomain() = RequestOtpRequest(mobileNumber)

fun VerifyOtpRequestDto.toDomain() = VerifyOtpRequest(mobileNumber, otp)

/**
 * Parses a `YYYY-MM-DD` field, rejecting anything else as a bad request.
 *
 * `LocalDate.parse` throws DateTimeParseException, which is not an [ApiError]
 * and so answers 500 — telling the client the server broke when in fact it sent
 * a birthday of "yesterday".
 */
fun parseDate(raw: String, field: String): LocalDate =
    runCatching { LocalDate.parse(raw) }.getOrElse {
        throw ApiError.badRequest("$field must be a date as YYYY-MM-DD.")
    }

fun RegisterRequestDto.toDomain() = RegisterRequest(
    registrationId = registrationId,
    fullName = fullName.trim(),
    username = username.trim(),
    password = password,
    dateOfBirth = dateOfBirth?.let { parseDate(it, "dateOfBirth") },
    sex = sex?.toSex(),
    email = email?.trim()?.lowercase(),
)

fun BookingRequestDto.toDomain() =
    BookingRequest(slotId, reasonForVisit?.trim()?.takeIf { it.isNotEmpty() }, confirmedByPatient)

/** [appointmentId] comes from the path, not the body — see [RescheduleRequestDto]. */
fun RescheduleRequestDto.toDomain(appointmentId: String) =
    RescheduleRequest(appointmentId, requestedSlotId, note?.trim()?.takeIf { it.isNotEmpty() })

fun buildDoctorQuery(
    searchText: String?,
    specialtyWire: String?,
    building: String?,
) = DoctorQuery(
    searchText = searchText?.trim()?.takeIf { it.isNotEmpty() },
    specialty = specialtyWire?.takeIf { it.isNotBlank() }?.toSpecialty(),
    building = building?.trim()?.takeIf { it.isNotEmpty() },
)

