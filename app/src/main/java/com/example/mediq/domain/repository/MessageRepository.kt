package com.example.mediq.domain.repository

import com.example.mediq.domain.model.Message
import com.example.mediq.domain.model.MessageThread
import com.example.mediq.domain.model.Paged

/**
 * Threads and messages between a patient and the clinic.
 *
 * Writes throw, because there is nothing sensible to return and the caller has
 * to say something to the patient; list reads settle to an empty page when the
 * server cannot be reached, so an unreachable server looks like "no messages"
 * rather than an error screen. That split is the one the notification and
 * appointment repositories already use.
 */
interface MessageRepository {

    /** Threads that have at least one message, most recent activity first. */
    suspend fun getThreads(): Paged<MessageThread>

    suspend fun getThread(threadId: String): MessageThread

    /**
     * The clinic thread when [doctorId] is null, otherwise the thread with that
     * doctor. Idempotent — a second call returns the same thread, never a second
     * one, which is what lets the "new message" button be tapped twice safely.
     */
    suspend fun openThread(doctorId: String? = null): MessageThread

    /** Newest first; pass [cursor] to walk back through older messages. */
    suspend fun getMessages(threadId: String, cursor: String? = null): Paged<Message>

    suspend fun sendMessage(
        threadId: String,
        body: String,
        relatedAppointmentId: String? = null,
    ): Message

    /** Marks every message the *other* side sent as read. */
    suspend fun markRead(threadId: String)
}
