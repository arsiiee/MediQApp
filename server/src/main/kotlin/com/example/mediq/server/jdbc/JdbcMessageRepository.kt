package com.example.mediq.server.jdbc

import com.example.mediq.domain.model.*
import com.example.mediq.domain.repository.*
import com.zaxxer.hikari.HikariDataSource
import java.sql.ResultSet

class JdbcMessageRepository(dataSource: HikariDataSource) : JdbcRepositoryBase(dataSource), MessageRepository {

    override suspend fun findConversation(id: String): LoadState<MessageThread?> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM message_threads WHERE id = ?").use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                LoadState.Success(if (rs.next()) mapThread(rs) else null)
            }
        }
    }

    override suspend fun findConversationsForDoctor(doctorId: String): LoadState<List<MessageThread>> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM message_threads WHERE doctor_id = ? ORDER BY last_message_at DESC").use { ps ->
            ps.setString(1, doctorId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<MessageThread>()
                while (rs.next()) list.add(mapThread(rs))
                LoadState.Success(list)
            }
        }
    }

    override suspend fun findConversationsForPatient(patientId: String): LoadState<List<MessageThread>> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM message_threads WHERE patient_id = ? ORDER BY last_message_at DESC").use { ps ->
            ps.setString(1, patientId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<MessageThread>()
                while (rs.next()) list.add(mapThread(rs))
                LoadState.Success(list)
            }
        }
    }

    override suspend fun findConversationsForSecretary(secretaryId: String): LoadState<List<MessageThread>> = withConnection { conn ->
        // A secretary sees conversations for the doctor they are assigned to.
        conn.prepareStatement("""
            SELECT mt.* FROM message_threads mt
            WHERE mt.doctor_id = (SELECT u.assigned_doctor_id FROM users u WHERE u.id = ?)
            ORDER BY mt.last_message_at DESC
        """.trimIndent()).use { ps ->
            ps.setString(1, secretaryId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<MessageThread>()
                while (rs.next()) list.add(mapThread(rs))
                LoadState.Success(list)
            }
        }
    }

    override suspend fun findMessages(conversationId: String): LoadState<List<Message>> = withConnection { conn ->
        conn.prepareStatement("SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC").use { ps ->
            ps.setString(1, conversationId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<Message>()
                while (rs.next()) list.add(mapMessage(rs))
                LoadState.Success(list)
            }
        }
    }

    override suspend fun createMessage(message: Message): LoadState<Message> = withConnection { conn ->
        conn.prepareStatement("INSERT INTO messages (id, conversation_id, author_id, author_role, body, sent_at, read_at) VALUES (?, ?, ?, ?, ?, ?, ?)").use { ps ->
            ps.setString(1, message.id)
            ps.setString(2, message.conversationId)
            ps.setString(3, message.authorId)
            ps.setString(4, message.authorRole.name)
            ps.setString(5, message.body)
            ps.setTimestamp(6, toTimestamp(message.sentAt))
            ps.setTimestamp(7, message.readAt?.let { toTimestamp(it) })
            ps.executeUpdate()
            LoadState.Success(message)
        }
    }

    override suspend fun markAsRead(conversationId: String, userId: String): LoadState<Unit> = withConnection { conn ->
        conn.prepareStatement("UPDATE message_threads SET unread_count = 0 WHERE id = ?").use { ps ->
            ps.setString(1, conversationId)
            ps.executeUpdate()
        }
        LoadState.Success(Unit)
    }

    override suspend fun createConversation(thread: MessageThread): LoadState<MessageThread> = withConnection { conn ->
        conn.prepareStatement("INSERT INTO message_threads (id, patient_id, doctor_id, subject, last_message_at, unread_count, awaiting_action) VALUES (?, ?, ?, ?, ?, ?, ?)").use { ps ->
            ps.setString(1, thread.id)
            ps.setString(2, thread.patientId)
            ps.setString(3, thread.doctorId)
            ps.setString(4, thread.subject)
            ps.setTimestamp(5, toTimestamp(thread.lastMessageAt))
            ps.setInt(6, thread.unreadCount)
            ps.setBoolean(7, thread.awaitingAction)
            ps.executeUpdate()
        }
        LoadState.Success(thread)
    }

    private fun mapThread(rs: ResultSet): MessageThread =
        MessageThread(
            id = rs.getString("id"),
            patientId = rs.getString("patient_id"),
            doctorId = rs.getString("doctor_id"),
            subject = rs.getString("subject"),
            lastMessageAt = fromTimestamp(rs.getTimestamp("last_message_at"))!!,
            unreadCount = rs.getInt("unread_count"),
            awaitingAction = rs.getBoolean("awaiting_action"),
        )

    private fun mapMessage(rs: ResultSet): Message =
        Message(
            id = rs.getString("id"),
            conversationId = rs.getString("conversation_id"),
            authorId = rs.getString("author_id"),
            authorRole = MessageAuthorRole.valueOf(rs.getString("author_role")),
            body = rs.getString("body"),
            sentAt = fromTimestamp(rs.getTimestamp("sent_at"))!!,
            readAt = fromTimestamp(rs.getTimestamp("read_at")),
        )
}
