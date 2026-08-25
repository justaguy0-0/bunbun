package com.example.bunbun.push

import android.Manifest
import android.annotation.SuppressLint
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
import com.example.bunbun.MainActivity
import com.example.bunbun.R

object BunbunNotifications {
    const val CHANNEL_ID = MESSAGE_NOTIFICATION_CHANNEL_ID

    data class State(
        val notificationsEnabled: Boolean,
        val permissionGranted: Boolean,
        val channelExists: Boolean,
        val channelImportance: Int?,
    )

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun state(context: Context): State {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        return State(
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
            channelExists = channel != null,
            channelImportance = channel?.importance,
        )
    }

    @SuppressLint("MissingPermission")
    fun notify(context: Context, command: PushNotificationCommand) {
        val payload = command.payload
        val target = ChatNavigationTarget.fromPayload(payload)
        val intent = target.putInto(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            (payload.chatId xor (payload.chatId ushr 32)).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val preview = payload.preview ?: context.getString(R.string.notification_new_message)
        val notification = NotificationCompat.Builder(context, command.channelId)
            .setSmallIcon(R.drawable.ic_stat_bunbun)
            .setContentTitle(payload.senderName ?: context.getString(R.string.app_name))
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(command.notificationId, notification)
    }

    fun clearAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
