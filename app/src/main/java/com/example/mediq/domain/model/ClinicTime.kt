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

private const val MINUTES_PER_DAY = 24 * 60

/**
 * The next session in [hours] that opens at or after [now], or null when the
 * doctor has none.
 *
 * The doctors list draws `Next open: Tue 09:00` on each row, and this is what
 * answers it. It reads the weekly [ClinicHours] the list already holds rather
 * than asking the server, because the alternative is one availability request
 * per row - an N+1 read on a list, for a value the list already has. The count
 * the figure also draws (`4 slots`) is deliberately **not** answered here: that
 * is a count of bookable slots for one date, which only `getAvailability` knows.
 *
 * "Next" is measured in the clinic's zone, from its day and time, not the
 * phone's. A session that has already opened today is next week's - it is not
 * offered as "next open" while the doctor is mid-clinic - and a session opening
 * at exactly [now] counts as today's.
 */
fun nextClinicSession(hours: List<ClinicHours>, now: Instant): ClinicHours? {
    if (hours.isEmpty()) return null
    val nowAtClinic = now.atZone(CLINIC_ZONE)
    val today = nowAtClinic.dayOfWeek
    val timeNow = nowAtClinic.toLocalTime()

    return hours.minByOrNull { session ->
        val daysAhead = (session.dayOfWeek.value - today.value + 7) % 7
        val minutesAhead = session.opensAt.toSecondOfDay() / 60
        if (daysAhead == 0 && session.opensAt.isBefore(timeNow)) {
            (7 * MINUTES_PER_DAY) + minutesAhead
        } else {
            daysAhead * MINUTES_PER_DAY + minutesAhead
        }
    }
}