package com.example.mediq.ui.screens.doctordetails

import com.example.mediq.data.model.Appointment
import com.example.mediq.data.model.AppointmentStatus
import com.example.mediq.data.model.ClinicHours
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val SLOT_INTERVAL_MINUTES = 30

internal fun time24hToMinutes(time: String): Int {
    val clean = time.trim().take(5)
    val parts = clean.split(":")
    if (parts.size < 2) return 0
    val h = parts[0].toIntOrNull() ?: 0
    val m = parts[1].toIntOrNull() ?: 0
    return h * 60 + m
}

internal fun minutesTo12h(minutes: Int): String {
    val safe = ((minutes % (24 * 60)) + 24 * 60) % (24 * 60)
    val h = safe / 60
    val m = safe % 60
    val amPm = if (h < 12) "AM" else "PM"
    val displayH = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "$displayH:${m.toString().padStart(2, '0')} $amPm"
}

internal fun time12hToMinutes(time: String): Int {
    val parts = time.trim().split(" ")
    if (parts.size < 2) return time24hToMinutes(time)
    val (timePart, amPmRaw) = parts[0] to parts[1].uppercase()
    val hm = timePart.split(":")
    var h = hm.getOrNull(0)?.toIntOrNull() ?: 0
    val m = hm.getOrNull(1)?.toIntOrNull() ?: 0
    if (amPmRaw == "PM" && h != 12) h += 12
    if (amPmRaw == "AM" && h == 12) h = 0
    return h * 60 + m
}

internal fun generateSlotLabelsForDay(clinicHoursForDay: List<ClinicHours>): List<String> {
    val labels = linkedSetOf<String>()
    clinicHoursForDay
        .filter { !it.isClosed }
        .sortedBy { time24hToMinutes(it.startTime) }
        .forEach { ch ->
            val start = time24hToMinutes(ch.startTime)
            val end = time24hToMinutes(ch.endTime)
            if (end <= start) return@forEach
            var slot = start
            while (slot + SLOT_INTERVAL_MINUTES <= end) {
                labels += minutesTo12h(slot)
                slot += SLOT_INTERVAL_MINUTES
            }
        }
    return labels.toList()
}

private fun parseLocalDateTime(value: String): LocalDateTime? {
    return try {
        LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(value))
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(value)
        } catch (_: Exception) {
            null
        }
    }
}

private fun patientHasAppointmentOnDay(
    date: LocalDate?,
    patientAppointments: List<Appointment>
): Boolean {
    if (date == null || patientAppointments.isEmpty()) return false
    return patientAppointments.any { appt ->
        if (appt.status == AppointmentStatus.CANCELLED) return@any false
        val ldt = parseLocalDateTime(appt.startsAt) ?: return@any false
        ldt.toLocalDate() == date
    }
}

internal fun buildSlotOptionsForDay(
    clinicHoursForDay: List<ClinicHours>,
    isToday: Boolean = false,
    nowMinutes: Int = 0,
    userBookedTimes: Set<String> = emptySet(),
    otherBookedTimes: Set<String> = emptySet(),
    date: LocalDate? = null,
    patientAppointments: List<Appointment> = emptyList()
): List<TimeSlotOption> {
    return generateSlotLabelsForDay(clinicHoursForDay).map { time ->
        val slotMinutes = time12hToMinutes(time)
        val isPast = isToday && slotMinutes <= nowMinutes
        val patientConflict = !isPast && patientHasAppointmentOnDay(date, patientAppointments)
        val status = when {
            isPast -> TimeSlotStatus.BLOCKED
            patientConflict -> TimeSlotStatus.USER_BOOKED
            time in userBookedTimes -> TimeSlotStatus.USER_BOOKED
            time in otherBookedTimes -> TimeSlotStatus.BLOCKED
            else -> TimeSlotStatus.AVAILABLE
        }
        TimeSlotOption(time = time, status = status)
    }
}

/** Back-compat helper for previews/tests that only know available/blocked labels. */
internal fun buildSlotStatuses(
    slotLabels: List<String>,
    available: Set<String>,
    blocked: Set<String>
): Map<String, TimeSlotStatus> = slotLabels.associateWith { time ->
    when {
        time in available -> TimeSlotStatus.AVAILABLE
        time in blocked -> TimeSlotStatus.BLOCKED
        else -> TimeSlotStatus.RESERVED
    }
}
