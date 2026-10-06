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
 * All enum wire-value lookups use firstOrNull with a safe fallback so that an
 * unknown value from the server doesn't crash the app — it falls back to a
 * sensible default (typically the first enum entry) and logs nothing, keeping
 * the UI usable while the field is ignored.
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
    role = UserRole.entries.firstOrNull { it.wireValue == role } ?: UserRole.PATIENT,
)

// ─── Doctors ─────────────────────────────────────────────────────────────────

fun DoctorDto.toDomain(): Doctor = Doctor(
    id = id,
    fullName = fullName,
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
    status = SlotStatus.entries.firstOrNull { it.wireValue == status } ?: SlotStatus.BLOCKED,
)

// ─── Appointments ─────────────────────────────────────────────────────────────

fun AppointmentDto.toDomain() = Appointment(
    id = id,
    doctor = doctor.toDomain(),
    startsAt = Instant.parse(startsAt),
    endsAt = Instant.parse(endsAt),
    status = AppointmentStatus.entries.firstOrNull { it.wireValue == status }
        ?: AppointmentStatus.PENDING_CONFIRMATION,
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
