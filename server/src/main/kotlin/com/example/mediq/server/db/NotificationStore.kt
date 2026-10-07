package com.example.mediq.server.db

import com.example.mediq.domain.model.Notification
import com.example.mediq.domain.model.NotificationType
import com.example.mediq.server.ApiError
import com.example.mediq.server.Database
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class NotificationStore(private val db: Database) {

    fun list(userId: String, limit: Int, offset: Int): Pair<List<Notification>, Boolean> =
        db.read { c ->
            val rows = c.prepareStatement(
                """
                SELECT id, type, title, body, created_at, read_at, related_appointment_id
                FROM notifications
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """.trimIndent()
            ).use { st ->
                st.setString(1, userId)
                st.setInt(2, limit + 1)
                st.setInt(3, offset)
                st.executeQuery().use { rs -> generateSequence { if (rs.next()) rs.readNotification() else null }.toList() }
            }
            rows.take(limit) to (rows.size > limit)
        }

    /**
     * Marks one notification read.
     *
     * Scoped by `user_id` in the WHERE clause, so passing someone else's id
     * matches no rows rather than marking it. `read_at` is not overwritten on
     * a second call, which keeps "first read time" honest.
     */
    fun markRead(userId: String, notificationId: String) = db.tx { c ->
        c.prepareStatement(
            "UPDATE notifications SET read_at = ? WHERE id = ? AND user_id = ? AND read_at IS NULL"
        ).use { st ->
            st.setObject(1, OffsetDateTime.now(ZoneOffset.UTC))
            st.setString(2, notificationId)
            st.setString(3, userId)
            val updated = st.executeUpdate()
            if (updated == 0) {
                val exists = c.prepareStatement(
                    "SELECT 1 FROM notifications WHERE id = ? AND user_id = ?"
                ).use { check ->
                    check.setString(1, notificationId)
                    check.setString(2, userId)
                    check.executeQuery().use { rs -> rs.next() }
                }
                if (!exists) throw ApiError.notFound("That notification was not found.")
            }
        }
    }
}

private fun java.sql.ResultSet.readNotification() = Notification(
    id = getString("id"),
    type = NotificationType.entries.first { it.wireValue == getString("type") },
    title = getString("title"),
    body = getString("body"),
    createdAt = getObject("created_at", OffsetDateTime::class.java).toInstant(),
    readAt = getObject("read_at", OffsetDateTime::class.java)?.toInstant(),
    relatedAppointmentId = getString("related_appointment_id"),
)