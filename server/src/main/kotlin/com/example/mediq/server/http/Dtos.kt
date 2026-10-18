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
import com.example.mediq.server.db.NewDoctorPractice
import com.example.mediq.server.db.NewStaffAccount
import com.example.mediq.server.db.UserRow
import com.example.mediq.server.db.UserStatus
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
    /**
     * When the booking was made, from `appointments.created_at`.
     *
     * Sent on the list and the single-appointment response alike: it is one
     * column the query already reads, and a second query for one row would cost
     * more contract than the row is worth. Nullable because a client running an
     * older build must be able to read a response without it — Gson cannot tell
     * "not sent" from "renamed", so the client hides its row rather than
     * guessing.
     */
    val createdAt: String? = null,
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

/**
 * One account as the admin area's directories see it.
 *
 * `status` is here because `users` now has the column: the admin screen's
 * Active/Inactive badge and its Enable/Disable buttons are the reason the column
 * exists, and the value is load-bearing — a disabled account is refused at
 * sign-in and its live tokens stop working.
 *
 * `lastActiveOn` is still absent, and the reason is worth keeping: the only
 * activity the server records is `sessions.created_at`, which is when an account
 * last *signed in*, not when it last did anything. Reporting it under a "last
 * active" heading would overstate what is known. Nothing stores per-request
 * activity, so there is nothing honest to send.
 *
 * `specializationId` and `licenseNumber` are absent for a different reason:
 * they belong to the `doctors` row, not the account, and `GET /doctors` already
 * publishes both. The link below is what lets a client join them.
 */
@Serializable
data class AdminAccountDto(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String?,
    val mobileNumber: String,
    val dateOfBirth: String,
    val role: String,
    /** `active` or `inactive`. See [AdminAccountDto] — this is what disables an account. */
    val status: String,
    /** The `doctors` row this staff account sits behind; null for patients and for staff with no doctor. */
    val doctorId: String? = null,
    /** When the account was created. ISO instant, as the admin screens' "joined on". */
    val createdAt: String,
)

/**
 * The whole account directory in one response.
 *
 * Three lists rather than a `?role=` parameter because the admin screens hold
 * all three at once — the dashboard counts across them and the directory pages
 * each read one — so one response is one round trip instead of three or four.
 * It is also the shape the fixture it replaces had, which is what keeps the
 * change on the web side a swap of the data source.
 */
@Serializable
data class AdminAccountsDto(
    val patients: List<AdminAccountDto>,
    val doctors: List<AdminAccountDto>,
    val secretaries: List<AdminAccountDto>,
)

/**
 * A specialty, how many doctors are under it, and what it covers.
 *
 * The list is projected from the [Specialty] enum that the patient app's filter
 * also uses, so a specialty cannot be added through the admin area that the app
 * would then not offer — which is what keeps two screens from disagreeing about
 * what the clinic practises. `description` is read from the `specialties` row
 * instead, because prose is the one part of this that is content rather than
 * contract, and it is null when the row carries no text.
 */
@Serializable
data class AdminSpecializationDto(
    val id: String,
    val name: String,
    val doctorCount: Int,
    val description: String? = null,
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
 * Enables or disables an account.
 *
 * A string rather than the enum, so an unrecognised value is a 400 the caller
 * can read and correct instead of a serialization failure that names a Kotlin
 * class. `Routes.kt` maps it through [UserStatus.fromWire] and names the two
 * accepted values in the message.
 */
@Serializable
data class UpdateAccountStatusRequestDto(val status: String)

/**
 * Body for creating a staff account.
 *
 * `practice` is nested rather than nine more flat fields, because those nine are
 * one thing: a doctor's `doctors` row. Nesting is what lets the server say
 * "practice details are for a doctor account" about a request that sent a fee
 * with no licence, instead of quietly writing half a practice record. The web's
 * create form sends it as one object for the same reason.
 *
 * `status` is optional and defaults to active. It is not a formality: the admin
 * form lets a new hire start disabled, and this is the same column the
 * Enable/Disable buttons write.
 */
@Serializable
data class CreateAccountRequestDto(
    val username: String,
    val password: String,
    val fullName: String,
    val email: String? = null,
    val mobileNumber: String,
    val dateOfBirth: String,
    val role: String,
    val status: String? = null,
    /** A secretary's doctor. Absent for every other role. */
    val assignedDoctorId: String? = null,
    /** A doctor's practice record. Absent for every other role. */
    val practice: DoctorPracticeDto? = null,
)

/** The `doctors` row a created doctor account sits behind. */
@Serializable
data class DoctorPracticeDto(
    val specializationId: String,
    val licenseNumber: String,
    val yearsExperience: Int,
    val feeCentavos: Long,
    val bio: String,
    val building: String,
    val floor: String,
    val room: String,
    val languages: List<String>,
)

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

// --- Doctor Workspace DTOs --------------------------------------------------

@Serializable
data class DoctorProfileDto(
    val id: String,
    val name: String,
    val email: String,
    val specialization: String,
    val licenseNumber: String,
    val joinedOn: String,
    val bio: String,
)

@Serializable
data class UpdateDoctorProfileDto(
    val name: String,
    val email: String,
    val bio: String,
)

@Serializable
data class DoctorAppointmentDto(
    val id: String,
    val patientId: String,
    val doctorId: String,
    val startsAt: String,
    val durationMinutes: Int,
    val status: String,
    val reason: String,
)

@Serializable
data class RecordOutcomeRequestDto(
    val outcome: String,
)

@Serializable
data class ScheduleBlockDto(
    val startTime: String,
    val endTime: String,
)

@Serializable
data class ScheduleDayDto(
    val dayOfWeek: Int,
    val sessions: List<ScheduleBlockDto>,
)

@Serializable
data class DoctorScheduleDto(
    val days: List<ScheduleDayDto>,
)

@Serializable
data class SaveScheduleRequestDto(
    val days: List<ScheduleDayDto>,
)

@Serializable
data class DoctorPatientDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val dateOfBirth: String,
    val address: String,
    val registeredOn: String,
    val status: String,
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
    createdAt = createdAt?.toString(),
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

/** What the admin area is shown about an account. See [AdminAccountDto] for what is left out. */
fun UserRow.toAdminDto() = AdminAccountDto(
    id = id,
    username = username,
    fullName = fullName,
    email = email,
    mobileNumber = mobileNumber,
    dateOfBirth = dateOfBirth.toString(),
    role = role.wireValue,
    status = status.wireValue,
    doctorId = doctorId,
    createdAt = createdAt.toString(),
)

fun UserRow.toDoctorPatientDto() = DoctorPatientDto(
    id = id,
    name = fullName,
    email = email ?: "",
    phone = mobileNumber,
    dateOfBirth = dateOfBirth.toString(),
    address = address ?: "",
    registeredOn = createdAt.toString().slice(0..9),
    status = status.wireValue,
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

/**
 * Maps the create form onto what [StaffAccountService] validates and writes.
 *
 * Wire values are parsed here — role, specialty, language, date — because this
 * is where an unrecognised one becomes a 400 naming the values that are valid.
 * The cross-field rules (which fields a doctor has to have, whether a username
 * is already taken) belong to the service, and are answered there in the
 * administrator's own words.
 */
fun CreateAccountRequestDto.toDomain() = NewStaffAccount(
    username = username,
    password = password,
    fullName = fullName.trim(),
    email = email?.trim()?.takeIf { it.isNotEmpty() },
    mobileNumber = mobileNumber,
    dateOfBirth = parseDate(dateOfBirth, "dateOfBirth"),
    role = role.toUserRole(),
    status = status?.let { wire ->
        UserStatus.fromWire(wire)
            ?: throw ApiError.badRequest(
                "status must be '${UserStatus.ACTIVE.wireValue}' or '${UserStatus.INACTIVE.wireValue}'."
            )
    } ?: UserStatus.ACTIVE,
    assignedDoctorId = assignedDoctorId?.trim()?.takeIf { it.isNotEmpty() },
    practice = practice?.toDomain(),
)

fun DoctorPracticeDto.toDomain() = NewDoctorPractice(
    specialty = specializationId.trim().toSpecialty(),
    licenseNumber = licenseNumber.trim(),
    yearsExperience = yearsExperience,
    feeCentavos = feeCentavos,
    bio = bio.trim(),
    building = building.trim(),
    floor = floor.trim(),
    room = room.trim(),
    languages = languages.map { it.trim().toConsultationLanguage() },
)

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

