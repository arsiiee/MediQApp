package com.example.mediq.data.repository

import com.example.mediq.data.api.MediQApiService
import com.example.mediq.data.api.call
import com.example.mediq.data.api.toDomain
import com.example.mediq.domain.model.ApiFailure
import com.example.mediq.domain.model.Notification
import com.example.mediq.domain.model.Paged
import com.example.mediq.domain.repository.NotificationRepository

/**
 * Live implementation of [NotificationRepository] backed by the Ktor server.
 *
 * [getNotifications] returns an empty page when the server is unreachable, so
 * that state shows an empty notification list rather than an error.
 * [markRead] propagates [ApiFailure] to the caller.
 */
class RetrofitNotificationRepository(
    private val api: MediQApiService,
) : NotificationRepository {

    override suspend fun getNotifications(): Paged<Notification> =
        try {
            call { api.getNotifications() }.toDomain { toDomain() }
        } catch (e: ApiFailure) {
            if (e.isNetworkFailure) Paged(emptyList()) else throw e
        }

    override suspend fun markRead(notificationId: String) {
        call { api.markNotificationRead(notificationId) }
    }
}
