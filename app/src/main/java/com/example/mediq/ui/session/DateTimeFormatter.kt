package com.example.mediq.ui.session

import com.example.mediq.data.model.ClinicLocation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter as JdkDateTimeFormatter
import java.util.Locale

/**
 * Shared date, time and location formatting for appointment surfaces (Home,
 * Appointments, Booking).
 *
 * All parsing accepts ISO-8601 strings from the API, with or without a UTC
 * offset. Instants are rendered in the device time zone and the device
 * locale, so a patient sees their appointment in local time regardless of how
 * the server serialized it.
 */
object DateTimeFormatter {

    /** "Mon, Oct 19" — short date used on the home card and appointments list. */
    fun formatDate(iso: String, locale: Locale = Locale.getDefault()): String {
        return parse(iso).format(JdkDateTimeFormatter.ofPattern("EEE, MMM d", locale))
    }

    /** "2:00 PM" — 12-hour clock matching the slot picker, used everywhere. */
    fun formatTime(iso: String, locale: Locale = Locale.getDefault()): String {
        return parse(iso).format(JdkDateTimeFormatter.ofPattern("h:mm a", locale))
    }

    /** "Monday, September 14, 2026" — long date used by Booking/Details. */
    fun formatDateLong(iso: String, locale: Locale = Locale.getDefault()): String {
        return parse(iso).format(JdkDateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))
    }

    /**
     * "Main Building · Floor 2 · Room 204".
     *
     * Floor and room are omitted when missing instead of printing a stray
     * separator.
     */
    fun formatLocation(location: ClinicLocation?): String {
        if (location == null) return ""
        val parts = mutableListOf<String>()
        if (location.building.isNotBlank()) {
            parts.add(location.building)
        }
        if (location.floor.isNotBlank()) {
            parts.add("Floor ${location.floor}")
        }
        if (location.room.isNotBlank()) {
            parts.add("Room ${location.room}")
        }
        return parts.joinToString(" · ")
    }

    /** The appointment moment as an instant, for "is it upcoming?" comparisons. */
    fun instant(iso: String): Instant = parse(iso).toInstant()

    /**
     * "11:01 PM" if the instant is today, "Yesterday" if yesterday, or "Oct 26"
     * otherwise. Returns an empty string when the timestamp cannot be parsed.
     *
     * Handles ISO-8601 strings with micro/nanosecond precision and any offset,
     * rendered in the device time zone.
     */
    fun formatMessageTime(
        iso: String,
        locale: Locale = Locale.getDefault()
    ): String {
        val zoned = parseOrNull(iso) ?: return ""
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val today = now.toLocalDate()
        val yesterday = today.minusDays(1)
        val messageDate = zoned.toLocalDate()

        return when {
            messageDate == today ->
                zoned.format(JdkDateTimeFormatter.ofPattern("h:mm a", locale))
            messageDate == yesterday ->
                "Yesterday"
            else ->
                zoned.format(JdkDateTimeFormatter.ofPattern("MMM d", locale))
        }
    }

    /** Parses an ISO-8601 timestamp, returning null when it cannot be parsed. */
    private fun parseOrNull(iso: String): ZonedDateTime? {
        return try {
            OffsetDateTime.parse(iso, JdkDateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .atZoneSameInstant(ZoneId.systemDefault())
        } catch (e: java.time.DateTimeException) {
            try {
                ZonedDateTime.parse(iso, JdkDateTimeFormatter.ISO_DATE_TIME)
            } catch (e2: java.time.DateTimeException) {
                try {
                    LocalDateTime.parse(iso, JdkDateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(ZoneId.systemDefault())
                } catch (e3: java.time.DateTimeException) {
                    try {
                        LocalDate.parse(iso, JdkDateTimeFormatter.ISO_LOCAL_DATE)
                            .atStartOfDay(ZoneId.systemDefault())
                    } catch (e4: java.time.DateTimeException) {
                        null
                    }
                }
            }
        }
    }

    private fun parse(iso: String): ZonedDateTime {
        return try {
            // Offset or "Z" present, e.g. "2026-10-19T14:00:00Z".
            OffsetDateTime.parse(iso, JdkDateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .atZoneSameInstant(ZoneId.systemDefault())
        } catch (e: java.time.DateTimeException) {
            try {
                // Named time zone, e.g. "2026-10-19T14:00:00 America/Chicago".
                ZonedDateTime.parse(iso, JdkDateTimeFormatter.ISO_DATE_TIME)
            } catch (e2: java.time.DateTimeException) {
                try {
                    // Naive local time, e.g. "2026-10-19T14:00:00".
                    LocalDateTime.parse(iso, JdkDateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(ZoneId.systemDefault())
                } catch (e3: java.time.DateTimeException) {
                    // Date only, e.g. "2026-09-21".
                    LocalDate.parse(iso, JdkDateTimeFormatter.ISO_LOCAL_DATE)
                        .atStartOfDay(ZoneId.systemDefault())
                }
            }
        }
    }
}
