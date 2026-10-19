package com.example.mediq.fake

import com.example.mediq.domain.model.Message
import com.example.mediq.domain.model.MessageThread
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.repository.MessageRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * A hand-written [MessageRepository] for ViewModel tests.
 *
 * [sentBodies] records the text of every message that actually reached the
 * repository, because "the composer cleared" and "the message was sent" are
 * different claims and only one of them is usually the thing under test.
 *
 * [sendGate], when set, parks [sendMessage] until the test releases it. That is
 * what makes a double tap observable: without it the first send would complete
 * instantly, the button would already be disabled, and a "cannot send twice"
 * test would pass whether or not the ViewModel guards anything.
 */
class FakeMessageRepository : MessageRepository {

    // --- Settable outcomes ---------------------------------------------------

    var threads: List<MessageThread> = emptyList()
    var getThreadsError: Throwable? = null

    /** Read by [getThread]; leave null to fail loudly if it was not expected. */
    var thread: MessageThread? = null
    var getThreadError: Throwable? = null

    /** Returned by [openThread]; leave null to fail loudly if not expected. */
    var openThreadResult: MessageThread? = null
    var openThreadError: Throwable? = null

    var messages: List<Message> = emptyList()
    var getMessagesError: Throwable? = null
    var nextCursor: String? = null

    /** Returned by [sendMessage]; leave null to fail loudly if not expected. */
    var sendResult: Message? = null
    var sendError: Throwable? = null
    var sendGate: CompletableDeferred<Unit>? = null

    var markReadError: Throwable? = null

    // --- What the ViewModel asked for ---------------------------------------

    val sentBodies = mutableListOf<String>()
    val sentAppointmentIds = mutableListOf<String?>()
    val markedReadThreadIds = mutableListOf<String>()
    val openedDoctorIds = mutableListOf<String?>()
    val requestedCursors = mutableListOf<String?>()

    var threadsRequested = false
        private set

    var messagesRequested = false
        private set

    /** True when no list read reached the repository at all. */
    val wasCalledAtAll: Boolean get() = threadsRequested || messagesRequested

    /** True when no mutation reached the repository at all. */
    val wasMutated: Boolean
        get() = sentBodies.isNotEmpty() ||
            markedReadThreadIds.isNotEmpty() ||
            openedDoctorIds.isNotEmpty()

    override suspend fun getThreads(): Paged<MessageThread> {
        threadsRequested = true
        getThreadsError?.let { throw it }
        return Paged(threads)
    }

    override suspend fun getThread(threadId: String): MessageThread {
        getThreadError?.let { throw it }
        return thread ?: error("FakeMessageRepository.thread was not set")
    }

    override suspend fun openThread(doctorId: String?): MessageThread {
        openedDoctorIds += doctorId
        openThreadError?.let { throw it }
        return openThreadResult ?: error("FakeMessageRepository.openThreadResult was not set")
    }

    override suspend fun getMessages(threadId: String, cursor: String?): Paged<Message> {
        messagesRequested = true
        requestedCursors += cursor
        getMessagesError?.let { throw it }
        return Paged(messages, nextCursor)
    }

    override suspend fun sendMessage(
        threadId: String,
        body: String,
        relatedAppointmentId: String?,
    ): Message {
        // Before recording, so a second call arriving while the first is parked
        // is caught rather than racing to completion.
        sendGate?.await()
        sentBodies += body
        sentAppointmentIds += relatedAppointmentId
        sendError?.let { throw it }
        return sendResult ?: error("FakeMessageRepository.sendResult was not set")
    }

    override suspend fun markRead(threadId: String) {
        markedReadThreadIds += threadId
        markReadError?.let { throw it }
    }
}
