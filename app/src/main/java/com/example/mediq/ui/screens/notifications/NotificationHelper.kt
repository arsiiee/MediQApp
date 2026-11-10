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
 * shown on the Notifications screen:
 *
 * - appointment confirmed
 * - reminder (tomorrow)
 * - reschedule approved
 * - appointment cancelled
 *
 * These mirror the four [NotificationType] categories so a patient receives the
 * same update in the system shade as they see in the in-app list.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "mediq_appointments"
    private const val CHANNEL_NAME = "MediQ Appointments"

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
                ?: Intent(context, Class.forName("${context.packageName}.MainActivity")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }.also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted; notification cannot be posted.
        }
    }

    /** Posts all four appointment notification categories as real system notifications. */
    fun notifyAll(context: Context, items: List<NotificationItem>) {
        for ((index, item) in items.withIndex()) {
            notify(context, id = index + 1, title = item.title, message = item.message)
        }
    }
}
