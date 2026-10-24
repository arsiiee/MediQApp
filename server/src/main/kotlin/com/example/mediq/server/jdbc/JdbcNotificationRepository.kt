package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class JdbcNotificationRepository(dataSource: HikariDataSource) : JdbcRepositoryBase(dataSource), NotificationRepository {

    override suspend fun findByUserId(userId: String, unreadOnly: Boolean): LoadState<List<Notification>> = withConnection { conn ->
        var sql = "SELECT * FROM notifications WHERE user_id = ?"
        val params = mutableListOf<Any>(userId)
        if (unreadOnly) sql += " AND is_read = FALSE"
        sql += " ORDER BY created_at DESC"
        conn.prepareStatement(sql).use { ps ->
            bindParams(ps, params)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<Notification>()
                while (rs.next()) list.add(mapNotification(rs))
                LoadState.Success(list)
            }
        }
    }

    override suspend fun create(notification: Notification): LoadState<Notification> = withConnection { conn ->
        conn.prepareStatement("INSERT INTO notifications (id, user_id, type, title, body, data, is_read, created_at, read_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)").use { ps ->
            ps.setString(1, notification.id)
            ps.setString(2, notification.userId)
            ps.setString(3, notification.type.name)
            ps.setString(4, notification.title)
            ps.setString(5, notification.body)
            ps.setString(6, toJson<Map<String, String>?>(notification.data))
            ps.setBoolean(7, notification.isRead)
            ps.setTimestamp(8, toTimestamp(notification.createdAt))
            ps.setTimestamp(9, notification.readAt?.let { toTimestamp(it) })
            ps.executeUpdate()
            LoadState.Success(notification)
        }
    }

    override suspend fun markAsRead(id: String): LoadState<Unit> = withConnection { conn ->
        conn.prepareStatement("UPDATE notifications SET is_read = TRUE, read_at = ? WHERE id = ?").use { ps ->
            ps.setTimestamp(1, toTimestamp(nowUtc()))
            ps.setString(2, id)
            ps.executeUpdate()
        }
        LoadState.Success(Unit)
    }

    override suspend fun markAllAsRead(userId: String): LoadState<Unit> = withConnection { conn ->
        conn.prepareStatement("UPDATE notifications SET is_read = TRUE, read_at = ? WHERE user_id = ? AND is_read = FALSE").use { ps ->
            ps.setTimestamp(1, toTimestamp(nowUtc()))
            ps.setString(2, userId)
            ps.executeUpdate()
        }
        LoadState.Success(Unit)
    }

    private fun bindParams(ps: java.sql.PreparedStatement, params: List<Any>) {
        for ((i, p) in params.withIndex()) ps.setObject(i + 1, p)
    }

    private fun mapNotification(rs: ResultSet): Notification =
        Notification(
            id = rs.getString("id"),
            userId = rs.getString("user_id"),
            type = NotificationType.valueOf(rs.getString("type")),
            title = rs.getString("title"),
            body = rs.getString("body"),
            data = fromJson<Map<String, String>?>(rs.getString("data")),
            isRead = rs.getBoolean("is_read"),
            createdAt = fromTimestamp(rs.getTimestamp("created_at"))!!,
            readAt = fromTimestamp(rs.getTimestamp("read_at")),
        )
}
