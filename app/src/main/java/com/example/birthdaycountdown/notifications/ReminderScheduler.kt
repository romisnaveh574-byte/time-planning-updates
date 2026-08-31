package com.example.birthdaycountdown.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.birthdaycountdown.data.CountdownEntity
import com.example.birthdaycountdown.domain.CountdownCalculator
import java.time.*

class ReminderScheduler(private val context: Context) {
    private val alarm = context.getSystemService(AlarmManager::class.java)

    fun schedule(record: CountdownEntity) {
        val now = ZonedDateTime.now()
        val target = CountdownCalculator.snapshot(record, now).target.minusMinutes(record.reminderMinutesBefore.toLong())
        val trigger = target.toInstant().toEpochMilli().coerceAtLeast(System.currentTimeMillis() + 1000)
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, record.id)
            .putExtra(EXTRA_NAME, record.name).putExtra(EXTRA_IS_BIRTHDAY, record.type.name)
        val pending = PendingIntent.getBroadcast(context, record.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try { alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending) }
        catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending) }
    }

    fun cancel(id: Long) {
        val pending = PendingIntent.getBroadcast(context, id.hashCode(), Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) alarm.cancel(pending)
    }

    companion object { const val EXTRA_ID = "record_id"; const val EXTRA_NAME = "record_name"; const val EXTRA_IS_BIRTHDAY = "record_type" }
}
