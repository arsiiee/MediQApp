package com.example.mediq.domain.model

import java.time.Instant

/**
 * What a notification is about. The screen picks an icon from this, so the
 * server decides the category and the client decides how it looks.
 */
enum class NotificationType(val wireValue: String) {
    APPOINTMENT_CONFIRMED("appointment_confirmed"),
    APPOINTMENT_REMINDER("appointment_reminder"),
    APPOINTMENT_CANCELLED("appointment_cancelled"),
    APPOINTMENT_RESCHEDULED("appointment_rescheduled"),
    SYSTEM("system"),
}

data class Notification(
    val id: String,
    val type: NotificationType,
    /** Short heading, e.g. "Appointment confirmed". */
    val title: String,
    /** The body copy. The server composes it. */
    val body: String,
    /** When the notification was created, used to show "1 hour ago". */
    val createdAt: Instant,
    val readAt: Instant? = null,
    /** Set when tapping the notification should open something specific. */
    val relatedAppointmentId: String? = null,
) {
    val isRead: Boolean get() = readAt != null
}