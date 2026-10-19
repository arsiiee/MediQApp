package com.example.mediq.data.api

import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentStatus
import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.ClinicHours
import com.example.mediq.domain.model.ClinicLocation
import com.example.mediq.domain.model.ConsultationLanguage
import com.example.mediq.domain.model.Doctor
import com.example.mediq.domain.model.Message
import com.example.mediq.domain.model.MessageAuthorRole
import com.example.mediq.domain.model.MessageScope
import com.example.mediq.domain.model.MessageThread
import com.example.mediq.domain.model.Money
import com.example.mediq.domain.model.Notification
import com.example.mediq.domain.model.NotificationType
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.model.RegisterRequest
import com.example.mediq.domain.model.Sex
import com.example.mediq.domain.model.SlotStatus
import com.example.mediq.domain.model.Specialty
import com.example.mediq.domain.model.TimeSlot
import com.example.mediq.domain.model.UserProfile
import com.example.mediq.domain.model.UserRole
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Extension functions that convert API wire DTOs to domain models.
 *
 * Every enum lookup resolves by wire value. Where the enum has an `UNKNOWN`
 * member, that is the fallback; otherwise each mapper states why it cannot have
 * one.
 *
 * The earlier version fell back to the first enum entry — `BLOCKED` for slots,
 * `PENDING_CONFIRMATION` for appointments — which turned a wire value this build
 * did not recognise into a confident wrong answer. A cancelled appointment
 * rendered as awaiting confirmation; an unrecognised slot rendered as *bookable*.
 * Neither raised anything, so no log pointed at the rename that caused it.
 *
 * **Adding `UNKNOWN` to an enum is not always safe.** These live in
 * `domain/model/`, which `:server` compiles as `sharedDomain`, so the server
 * parses some of them from untrusted input and reads others back out of the
 * database with `.first {}`. `AppointmentStatus` and `SlotStatus` are safe:
 * nothing parses them from a request. `UserRole` and `Specialty` are not — see
 * the comments on those mappers.
 */

// ─── Auth ────────────────────────────────────────────────────────────────────

fun AuthSessionDto.toDomain(): AuthSession = AuthSession(
    accessToken = accessToken,
    expiresAt = Instant.parse(expiresAt),
    profile = profile.toDomain(),
)

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    username = username,
    fullName = fullName,
    email = email ?: "",
    mobileNumber = mobileNumber,
    dateOfBirth = LocalDate.parse(dateOfBirth),
    sex = sex?.let { w -> Sex.entries.firstOrNull { it.wireValue == w } },
    address = address,
    // No UNKNOWN fallback on role, unlike the other enums below. UserRole is
    // parsed from a JWT claim on the server and read back with `.first {}`, so
    // an UNKNOWN member would make the server accept a role it does not
    // recognise. PATIENT is the safe direction to be wrong in: it is the least
    // privileged role, and the app shows no staff-only surface.
    role = UserRole.entries.firstOrNull { it.wireValue == role } ?: UserRole.PATIENT,
)

/**
 * The reverse of [UserProfileDto.toDomain], for rewriting the cached session
 * after an edit.
 *
 * It exists only for that round trip, so it copies the domain's own rule that
 * blank means absent ([UserProfile.email] is a non-null `String` where the wire
 * type is nullable). `dateOfBirth` goes back out in the ISO form the server
 * sends and `TokenStore` stores, not a display format.
 */
fun UserProfile.toDto(): UserProfileDto = UserProfileDto(
    id = id,
    username = username,
    fullName = fullName,
    email = email.ifBlank { null },
    mobileNumber = mobileNumber,
    dateOfBirth = dateOfBirth.toString(),
    sex = sex?.wireValue,
    address = address,
    role = role.wireValue,
)

// ─── Doctors ─────────────────────────────────────────────────────────────────

fun DoctorDto.toDomain(): Doctor = Doctor(
    id = id,
    fullName = fullName,
    // No UNKNOWN fallback on specialty either. It is parsed from the public
    // `?specialty=` query param and read back with `.first {}` in DoctorStore,
    // so an UNKNOWN member would widen what the server accepts and then fail on
    // the row lookup. A mis-rendered specialty is cosmetic; that is not.
    specialty = Specialty.entries.firstOrNull { it.wireValue == specialty }
        ?: Specialty.INTERNAL_MEDICINE,
    yearsOfExperience = yearsOfExperience,
    consultationFee = Money(consultationFee.amountInCentavos),
    location = location.toDomain(),
    licenseNumber = licenseNumber,
    bio = bio,
    languages = languages.mapNotNull { w ->
        ConsultationLanguage.entries.firstOrNull { it.wireValue == w }
    },
    clinicHours = clinicHours.map { it.toDomain() },
)

fun ClinicLocationDto.toDomain() = ClinicLocation(
    building = building,
    floor = floor,
    room = room,
)

fun ClinicHoursDto.toDomain() = ClinicHours(
    dayOfWeek = DayOfWeek.of(dayOfWeek),
    opensAt = LocalTime.parse(opensAt),
    closesAt = LocalTime.parse(closesAt),
)

fun AvailableDateDto.toDomain() = AvailableDate(
    date = LocalDate.parse(date),
    openSlotCount = openSlotCount,
)

fun TimeSlotDto.toDomain() = TimeSlot(
    id = id,
    doctorId = doctorId,
    startsAt = Instant.parse(startsAt),
    endsAt = Instant.parse(endsAt),
    status = SlotStatus.entries.firstOrNull { it.wireValue == status } ?: SlotStatus.UNKNOWN,
)

// ─── Appointments ─────────────────────────────────────────────────────────────

fun AppointmentDto.toDomain() = Appointment(
    id = id,
    doctor = doctor.toDomain(),
    startsAt = Instant.parse(startsAt),
    endsAt = Instant.parse(endsAt),
    status = AppointmentStatus.entries.firstOrNull { it.wireValue == status }
        ?: AppointmentStatus.UNKNOWN,
    location = location.toDomain(),
    fee = Money(fee.amountInCentavos),
    reasonForVisit = reasonForVisit,
    // Guarded, not because the server is expected to send a bad timestamp but
    // because a value this build cannot read must not blank the screen: null
    // hides the `BOOKED ON` row, which is the honest rendering of "unknown".
    createdAt = createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
)

// ─── Notifications ────────────────────────────────────────────────────────────

fun NotificationDto.toDomain() = Notification(
    id = id,
    type = NotificationType.entries.firstOrNull { it.wireValue == type }
        ?: NotificationType.SYSTEM,
    title = title,
    body = body,
    createdAt = Instant.parse(createdAt),
    readAt = readAt?.let { Instant.parse(it) },
    relatedAppointmentId = relatedAppointmentId,
)

// ─── Messages ─────────────────────────────────────────────────────────────────

fun MessageThreadDto.toDomain() = MessageThread(
    id = id,
    // CLINIC is the safe direction to be wrong in: it renders the generic
    // "clinic" title instead of naming a doctor whose id this build may not
    // have matched. Unlike UserRole, no UNKNOWN member belongs here — these two
    // enums are read only by the client and never parsed back into the server.
    scope = MessageScope.entries.firstOrNull { it.wireValue == scope } ?: MessageScope.CLINIC,
    doctorId = doctorId,
    doctorName = doctorName,
    lastMessageAt = lastMessageAt?.let { Instant.parse(it) },
    lastMessagePreview = lastMessagePreview,
    unreadCount = unreadCount,
)

fun MessageDto.toDomain() = Message(
    id = id,
    threadId = threadId,
    body = body,
    // STAFF is the safe direction: the message renders with an author name and
    // can never be mistaken for something the patient wrote. The reverse would
    // put words in the patient's mouth.
    authorRole = MessageAuthorRole.entries.firstOrNull { it.wireValue == authorRole }
        ?: MessageAuthorRole.STAFF,
    authorName = authorName,
    createdAt = Instant.parse(createdAt),
    readAt = readAt?.let { Instant.parse(it) },
    relatedAppointmentId = relatedAppointmentId,
    mine = mine,
)

// ─── Paging ───────────────────────────────────────────────────────────────────

fun <T, R> PagedDto<T>.toDomain(mapper: T.() -> R): Paged<R> = Paged(
    items = items.map { it.mapper() },
    nextCursor = nextCursor,
)

// ─── Request mappers ──────────────────────────────────────────────────────────

fun RegisterRequest.toDto() = RegisterRequestDto(
    registrationId = registrationId,
    fullName = fullName,
    username = username,
    password = password,
    dateOfBirth = dateOfBirth?.toString(),
    sex = sex?.wireValue,
    email = email,
)
