package com.example.notifications

import android.Manifest
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
import com.example.MainActivity
import com.example.data.PushNotificationEvent

/**
 * Helper for creating Android Notification Channels and posting high-priority Push Notifications
 * when a new chat message arrives (`NEW_MESSAGE`) or when a friend invites a user to join a chat room (`ROOM_INVITE`).
 */
class PushNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_MESSAGES = "wanas_new_messages_channel"
        const val CHANNEL_ID_INVITES = "wanas_room_invites_channel"
    }

    init {
        ensureNotificationChannels()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "إشعارات الرسائل الجديدة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند تلقي رسالة جديدة في غرف الدردشة"
                enableVibration(true)
            }

            val invitesChannel = NotificationChannel(
                CHANNEL_ID_INVITES,
                "إشعارات دعوات غرف الدردشة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند دعوة صديق للانضمام إلى غرفة دردشة"
                enableVibration(true)
            }

            manager.createNotificationChannel(messagesChannel)
            manager.createNotificationChannel(invitesChannel)
        }
    }

    fun showPushNotification(event: PushNotificationEvent): Boolean {
        ensureNotificationChannels()
        if (!hasNotificationPermission()) {
            return false
        }

        val isInvite = event.notificationType == "ROOM_INVITE"
        val channelId = if (isInvite) CHANNEL_ID_INVITES else CHANNEL_ID_MESSAGES
        val title = if (isInvite) {
            "📩 دعوة غرفة دردشة من ${event.senderName}"
        } else {
            "💬 رسالة جديدة في «${event.roomName}»"
        }

        val body = if (isInvite) {
            "يدعوك ${event.senderName} للانضمام إلى غرفة «${event.roomName}»: ${event.messageBody}"
        } else {
            "${event.senderName}: ${event.messageBody}"
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_room_id", event.roomId)
            putExtra("extra_room_name", event.roomName)
            putExtra("extra_notification_type", event.notificationType)
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        val pendingIntent = PendingIntent.getActivity(
            context,
            event.notificationId.hashCode(),
            launchIntent,
            pendingFlags
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(
                if (isInvite) NotificationCompat.CATEGORY_SOCIAL else NotificationCompat.CATEGORY_MESSAGE
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context)
                .notify(event.notificationId.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
