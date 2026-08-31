package com.example.birthdaycountdown.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channel = NotificationChannel(CHANNEL, "生日和纪念日", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("时间提醒")
            .setContentText(intent.getStringExtra(ReminderScheduler.EXTRA_NAME) ?: "你的记录")
            .setAutoCancel(true).build()
        try { NotificationManagerCompat.from(context).notify(intent.getLongExtra(ReminderScheduler.EXTRA_ID, 0).toInt(), notification) } catch (_: SecurityException) { }
    }
    companion object { private const val CHANNEL = "countdown_reminders" }
}
