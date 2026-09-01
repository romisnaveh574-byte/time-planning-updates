package com.example.birthdaycountdown.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.birthdaycountdown.data.AppDatabase
import com.example.birthdaycountdown.data.CountdownRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = ReminderScheduler(context)
                CountdownRepository(AppDatabase.create(context).countdownDao()).allRecords()
                    .filter { it.reminderEnabled }
                    .forEach(scheduler::schedule)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
