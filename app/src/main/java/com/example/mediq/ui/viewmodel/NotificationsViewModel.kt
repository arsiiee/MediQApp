package com.example.mediq.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediq.data.api.SupabaseApiService
import com.example.mediq.data.model.Notification as ApiNotification
import com.example.mediq.data.model.NotificationType as ApiNotificationType
import com.example.mediq.ui.screens.notifications.NotificationHelper
import com.example.mediq.ui.screens.notifications.NotificationItem
import com.example.mediq.ui.screens.notifications.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Holds the patient's notifications loaded from the API and promotes unread ones
 * to real Android system notifications. Marks items read in Supabase when opened/viewed
 * so unread state does not keep reappearing.
 */
class NotificationsViewModel(private val context: Context) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val api = SupabaseApiService()
    /** IDs currently posted as system notifications (capped to recent unread max). */
    private val postedSystemNotificationIds = mutableSetOf<String>()
    private val systemNotificationMutex = Mutex()
    @Volatile private var markAllInFlight = false

    init {
        viewModelScope.launch {
            // Channel must exist before any notification is posted.
            NotificationHelper.init(context)
            loadNotifications(markAllRead = false)
        }
    }

    /** Loads notifications from the API. Optionally marks all as read after load. */
    fun loadNotifications(markAllRead: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val apiNotifications = api.getNotifications()
                val items = apiNotifications.map { toNotificationItem(it) }
                _notifications.value = items

                syncSystemNotifications(items)

                if (markAllRead && items.any { it.unread }) {
                    markAllAsReadOptimistic()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load notifications"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Posts system notifications for up to [NotificationHelper.MAX_SYSTEM_NOTIFICATIONS] most recent
     * unread items (API returns created_at.desc). Cancels only overflow/unread-outside-cap slots;
     * does not mark overflow items as read. Reconciles tracked posted IDs so items can re-enter cap.
     */
    private suspend fun syncSystemNotifications(items: List<NotificationItem>) {
        val allowedUnread = items.asSequence()
            .filter { it.unread }
            .take(NotificationHelper.MAX_SYSTEM_NOTIFICATIONS)
            .toList()
        val allowedIds = allowedUnread.map { it.id }.toSet()

        val toPost = mutableListOf<NotificationItem>()
        val toCancel = mutableListOf<String>()
        systemNotificationMutex.withLock {
            // Post newly allowed unread items not currently shown.
            allowedUnread.filter { postedSystemNotificationIds.add(it.id) }.forEach { toPost += it }
            // Cancel only slots that are no longer allowed (overflow/read/removed from current set).
            postedSystemNotificationIds.filter { it !in allowedIds }.forEach { toCancel += it }
            toCancel.forEach { postedSystemNotificationIds.remove(it) }
        }

        if (toPost.isNotEmpty()) {
            NotificationHelper.notifyUnread(context, toPost)
        }
        toCancel.forEach { NotificationHelper.cancel(context, it) }
    }

    /** Marks a single notification as read (persisted in Supabase) and updates UI. */
    fun markAsRead(id: String) {
        val wasUnread = _notifications.value.any { it.id == id && it.unread }
        if (!wasUnread) return

        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(unread = false) else it
        }
        viewModelScope.launch {
            systemNotificationMutex.withLock { postedSystemNotificationIds.remove(id) }
        }
        NotificationHelper.cancel(context, id)

        viewModelScope.launch {
            try {
                api.markNotificationRead(id)
            } catch (_: Exception) {
                // Keep UI responsive; reload can reconcile if needed.
            }
        }
    }

    /** Marks all notifications as read (persisted in Supabase). Safe to call from screen open. */
    fun markAllAsRead() {
        if (markAllInFlight) return
        if (_notifications.value.none { it.unread }) return
        markAllAsReadOptimistic()
    }

    private fun markAllAsReadOptimistic() {
        markAllInFlight = true
        val unreadIds = _notifications.value.filter { it.unread }.map { it.id }
        _notifications.value = _notifications.value.map { if (it.unread) it.copy(unread = false) else it }
        viewModelScope.launch {
            systemNotificationMutex.withLock { postedSystemNotificationIds.removeAll(unreadIds.toSet()) }
        }
        unreadIds.forEach { NotificationHelper.cancel(context, it) }

        viewModelScope.launch {
            try {
                api.markAllNotificationsRead()
            } catch (_: Exception) {
                // Best-effort; user can re-open to retry.
            } finally {
                markAllInFlight = false
            }
        }
    }

    private fun toNotificationItem(api: ApiNotification): NotificationItem =
        NotificationItem(
            id = api.id,
            type = toUiType(api.type),
            title = api.title,
            message = api.body,
            time = api.createdAt,
            unread = !api.isRead,
            appointmentId = api.data?.get("appointmentId")
                ?: api.data?.get("appointment_id")
                ?: api.data?.get("id")
        )

    private fun toUiType(type: ApiNotificationType): NotificationType = when (type) {
        ApiNotificationType.APPOINTMENT_CONFIRMED -> NotificationType.CONFIRMED
        ApiNotificationType.APPOINTMENT_BOOKED -> NotificationType.CONFIRMED
        ApiNotificationType.APPOINTMENT_REMINDER -> NotificationType.REMINDER
        ApiNotificationType.APPOINTMENT_CANCELLED -> NotificationType.CANCELLED
        else -> NotificationType.CONFIRMED
    }
}

