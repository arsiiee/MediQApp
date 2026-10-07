package com.example.mediq.data.api

import com.example.mediq.domain.model.Appointment
import com.example.mediq.domain.model.AppointmentStatus
import com.example.mediq.domain.model.AuthSession
import com.example.mediq.domain.model.AvailableDate
import com.example.mediq.domain.model.ClinicHours
import com.example.mediq.domain.model.ClinicLocation
import com.example.mediq.domain.model.ConsultationLanguage
import com.example.mediq.domain.model.Doctor
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
