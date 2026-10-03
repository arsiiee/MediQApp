package com.example.mediq.domain.repository

import com.example.mediq.domain.model.Notification
import com.example.mediq.domain.model.Paged

interface NotificationRepository {

    suspend fun getNotifications(): Paged<Notification>

    suspend fun markRead(notificationId: String)
}