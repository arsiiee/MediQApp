package com.example.mediq.domain.model

import java.time.Instant

/**
 * What a thread is with.
 *
 * A patient has exactly one [CLINIC] thread — one conversation with the front
 * desk, however many staff take turns answering it — and one [DOCTOR] thread per
 * doctor they have written to. There is deliberately no per-appointment scope:
 * that would give a patient a new thread for every booking.
 */
enum class MessageScope(val wireValue: String) {
    CLINIC("clinic"),
    DOCTOR("doctor"),
}

/**
 * Who wrote a message.
 *
 * Deliberately not [UserRole]: a patient is not told whether a reply came from a
 * doctor, a secretary, or an admin, and the internal role names are not a
 * contract the client should depend on.
 *
 * Neither enum here gets the usual `UNKNOWN` sentinel. That sentinel exists for
 * values the *server* may add that an older client does not know; both of these
 * sets are server-owned and closed, so a value that does not parse is a bug that
 * should fail loudly rather than render as "Unknown".
 */
enum class MessageAuthorRole(val wireValue: String) {
    PATIENT("patient"),
    STAFF("staff"),
}

/** The longest body the server accepts. The composer refuses to send longer. */
const val MAX_MESSAGE_LENGTH = 2000

/**
 * A conversation with the clinic or with one doctor.
 *
 * [lastMessageAt] and [lastMessagePreview] mirror the newest message; the server
 * keeps them so the thread list has an ordering key and a preview line without a
 * query per row.
 */
data class MessageThread(
    val id: String,
    val scope: MessageScope,
    /** Set for a [MessageScope.DOCTOR] thread, null for the clinic thread. */
    val doctorId: String? = null,
    val doctorName: String? = null,
    val lastMessageAt: Instant? = null,
    val lastMessagePreview: String? = null,
    /** Messages from the other side that this caller has not read yet. */
    val unreadCount: Int = 0,
) {
    val hasUnread: Boolean get() = unreadCount > 0
}

/**
 * One message in a thread.
 *
 * [mine] is decided by the server from the caller's identity — the client is not
 * sent other users' ids, and "is this mine" is the only question the UI asks of
 * the author.
 */
data class Message(
    val id: String,
    val threadId: String,
    val body: String,
    val authorRole: MessageAuthorRole,
    val authorName: String,
    val createdAt: Instant,
    /**
     * When the *other* side read it, or null when they have not. It is set by
     * the reader, not the writer, so my own messages are never unread to me
     * however long the reply takes.
     */
    val readAt: Instant? = null,
    /** The appointment this message is about, when it was sent from one. */
    val relatedAppointmentId: String? = null,
    val mine: Boolean = false,
) {
    val isUnreadByMe: Boolean get() = readAt == null && !mine
}
