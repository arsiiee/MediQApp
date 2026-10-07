package com.example.mediq.fake

import com.example.mediq.domain.model.Notification
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.repository.NotificationRepository

/**
 * A hand-written [NotificationRepository] for ViewModel tests.
 *
 * Records the ids passed to [markRead], because "the row was marked read" and "the
 * ViewModel reported it marked read" are different claims — and only one of them
 * is the behaviour under test. A state-only assertion would not tell them apart.
 */
class FakeNotificationRepository : NotificationRepository {

    // --- Settable outcomes ---------------------------------------------------

    /** Empty by default, so a test asserting on content must opt in explicitly. */
    var notifications: List<Notification> = emptyList()
    var getNotificationsError: Throwable? = null
    var nextCursor: String? = null
    var markReadError: Throwable? = null

    // --- What the ViewModel asked for ---------------------------------------

    val markedReadIds = mutableListOf<String>()

    /** True when no list read reached the repository at all. */
    val wasCalledAtAll: Boolean get() = notificationsRequested

    var notificationsRequested = false
        private set

    /** True when no mutation reached the repository at all. */
    val wasMutated: Boolean get() = markedReadIds.isNotEmpty()

    override suspend fun getNotifications(): Paged<Notification> {
        notificationsRequested = true
        getNotificationsError?.let { throw it }
        return Paged(notifications, nextCursor)
    }

    override suspend fun markRead(notificationId: String) {
        markedReadIds += notificationId
        markReadError?.let { throw it }
    }
}
