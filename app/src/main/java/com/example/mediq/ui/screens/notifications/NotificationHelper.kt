package com.example.mediq.ui.screens.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Posts real Android system notifications for the appointment-related events
 * shown on the Notifications screen.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "mediq_appointments"
    private const val CHANNEL_NAME = "MediQ Appointments"

    /** Maximum number of unread notifications to show as Android system notifications at once. */
    const val MAX_SYSTEM_NOTIFICATIONS = 5

    /** Creates the notification channel (Android 8+) required to post notifications. */
    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Updates about your appointments"
                enableLights(true)
                enableVibration(true)
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a system notification for a single [NotificationItem].
     *
     * Tapping the notification opens the app. The notification is auto-cancelled
     * once the user taps it.
     */
    @Suppress("MissingPermission")
    fun notify(context: Context, id: Int, title: String, message: String) {
        init(context)

        // Android 13+ requires the POST_NOTIFICATIONS runtime permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent().apply {
                    setClassName(context.packageName, "${context.packageName}.MainActivity")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; notification cannot be posted.
        }
    }

    /** Stable Android notification id derived from the API notification id. */
    fun stableNotificationId(id: String): Int = id.hashCode()

    /** Posts system notifications for unread items only (avoids re-popping read history). */
    fun notifyUnread(context: Context, items: List<NotificationItem>) {
        items.filter { it.unread }.forEach { item ->
            notify(context, id = stableNotificationId(item.id), title = item.title, message = item.message)
        }
    }

    /** Clears a posted system notification (e.g. after marking read). */
    fun cancel(context: Context, apiId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(stableNotificationId(apiId))
        } catch (_: Exception) {
        }
    }
}
