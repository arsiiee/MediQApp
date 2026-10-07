package com.example.mediq.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The clinic operates in one timezone, so every instant the server sends is
 * rendered in this zone rather than the phone's.
 *
 * Appointments are stored and sent as UTC [Instant]s. Converting to a display
 * date here — instead of letting each screen pick a zone — is what stops a
 * booking from landing on the wrong day for someone travelling, or for a phone
 * set to a different timezone than the clinic.
 */
val CLINIC_ZONE: ZoneId = ZoneId.of("Asia/Manila")

/** The local calendar date an instant falls on at the clinic. */
fun Instant.toClinicDate(): LocalDate = atZone(CLINIC_ZONE).toLocalDate()

/** The local wall-clock time an instant falls on at the clinic. */
fun Instant.toClinicTime(): LocalTime = atZone(CLINIC_ZONE).toLocalTime()

/** Builds an instant from a clinic-local date and time. */
fun clinicDateTime(date: LocalDate, time: LocalTime): Instant =
    date.atTime(time).atZone(CLINIC_ZONE).toInstant()

/** Whether an appointment has already happened, as of [now]. */
fun Instant.isPast(now: Instant): Boolean = isBefore(now)