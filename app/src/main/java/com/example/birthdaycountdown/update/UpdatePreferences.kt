package com.example.birthdaycountdown.update

import android.content.SharedPreferences

class UpdatePreferences(private val prefs: SharedPreferences) {
    fun shouldCheckToday(nowDay: Long): Boolean = prefs.getLong("update_check_day", -1) != nowDay
    fun markChecked(nowDay: Long) { prefs.edit().putLong("update_check_day", nowDay).apply() }
}

