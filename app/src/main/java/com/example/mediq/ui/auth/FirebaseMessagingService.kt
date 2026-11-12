package com.example.mediq.ui.auth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mediq.R
import com.example.mediq.data.api.SupabaseClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

val JSON_MEDIA_TYPE = "application/json".toMediaType()

class FirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val httpClient = OkHttpClient()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            saveFcmTokenToSupabase(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: "MediQ"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val clickAction = remoteMessage.data["click_action"] ?: ""

        showLocalNotification(title, body, clickAction)
    }

    private suspend fun saveFcmTokenToSupabase(token: String) {
        try {
            val userId = SupabaseClient.authUserId
            if (userId.isNullOrEmpty()) return

            val body = JSONObject().put("fcm_token", token).toString()
            val url = "${SupabaseClient.restUrl}/profiles?user_id=eq.$userId"
            val request = Request.Builder()
                .url(url)
                .method("PATCH", RequestBody.create(JSON_MEDIA_TYPE, body))
                .header("apikey", SupabaseClient.anonKey)
                .header("Authorization", "Bearer ${SupabaseClient.accessToken ?: ""}")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "unknown error"
                    android.util.Log.e("FirebaseMessaging", "Failed to save FCM token: $errorBody")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseMessaging", "Error saving FCM token", e)
        }
    }

    private fun showLocalNotification(title: String, body: String, clickAction: String) {
        val channelId = "mediq_notifications"
        val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MediQ Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Appointment and message notifications"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (clickAction.isNotEmpty()) {
                data = android.net.Uri.parse(clickAction)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.example.mediq.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager?.notify(0, notification)
    }
}
