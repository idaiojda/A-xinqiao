package com.example.xinqiao.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationUtils {
    const val CHANNEL_ID_CHECKIN = "checkin_reminder"
    const val CHANNEL_ID_MESSAGES = "messages"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val checkin = NotificationChannel(CHANNEL_ID_CHECKIN, "打卡提醒", NotificationManager.IMPORTANCE_DEFAULT)
            val messages = NotificationChannel(CHANNEL_ID_MESSAGES, "消息提醒", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(checkin)
            nm.createNotificationChannel(messages)
        }
    }

    fun showCheckinReminder(context: Context, title: String, text: String, id: Int = 1001) {
        ensureChannel(context)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID_CHECKIN)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notif)
    }

    fun showMessageNotification(context: Context, group: String, author: String?, text: String, id: Int = ((System.currentTimeMillis() % Int.MAX_VALUE).toInt())) {
        ensureChannel(context)
        val title = if (group.isNotBlank()) "$group 新消息" else "新消息"
        val content = if (!author.isNullOrBlank()) "$author: $text" else text
        val intent = android.content.Intent(context, com.example.xinqiao.activity.GroupChatActivity::class.java).apply {
            putExtra("group", group)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = androidx.core.app.TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(id, android.app.PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) android.app.PendingIntent.FLAG_IMMUTABLE else 0))

        val notif = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(id, notif)
    }
}
