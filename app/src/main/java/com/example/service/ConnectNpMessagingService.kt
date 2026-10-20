package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ConnectNpRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ConnectNpMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token: $token")
        ConnectNpRepository.setFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "ConnectNP Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "You have a new update on ConnectNP"

        val type = remoteMessage.data["type"] ?: "general"

        showLocalNotification(
            context = applicationContext,
            title = title,
            body = body,
            type = type
        )
    }

    companion object {
        private const val TAG = "ConnectNpFCM"
        const val CHANNEL_ID_MAIN = "connectnp_main_notifications"
        const val CHANNEL_ID_CHATS = "connectnp_chat_notifications"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val mainChannel = NotificationChannel(
                    CHANNEL_ID_MAIN,
                    "ConnectNP Social & Likes",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts for new likes, comments, and community updates"
                }

                val chatChannel = NotificationChannel(
                    CHANNEL_ID_CHATS,
                    "ConnectNP Messages",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Direct chat and voice message notifications"
                    enableVibration(true)
                }

                notificationManager.createNotificationChannel(mainChannel)
                notificationManager.createNotificationChannel(chatChannel)
            }
        }

        fun showLocalNotification(
            context: Context,
            title: String,
            body: String,
            type: String = "general"
        ) {
            createNotificationChannels(context)

            try {
                ConnectNpRepository.incrementUnreadNotifications()
            } catch (e: Exception) {
                // Ignore if repository is not yet ready
            }

            // Verify permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Notification permission not granted, skipping system banner")
                    return
                }
            }

            val channelId = if (type == "chat") CHANNEL_ID_CHATS else CHANNEL_ID_MAIN

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notification_type", type)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.connectnp_icon_full)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(
                    if (type == "chat") NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            try {
                val notificationManager = NotificationManagerCompat.from(context)
                val notificationId = (System.currentTimeMillis() % 100000).toInt()
                notificationManager.notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException while notifying: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to display notification: ${e.message}", e)
            }
        }
    }
}
