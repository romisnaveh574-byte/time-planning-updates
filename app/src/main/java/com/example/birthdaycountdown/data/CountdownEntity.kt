package com.example.birthdaycountdown.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecordType { BIRTHDAY, ANNIVERSARY }
enum class CalendarType { SOLAR, LUNAR }

@Entity(tableName = "countdown_records")
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RecordType,
    val name: String,
    val dateTimeIso: String,
    val calendarType: CalendarType = CalendarType.SOLAR,
    val lunarYear: Int? = null,
    val lunarMonth: Int? = null,
    val lunarDay: Int? = null,
    val lunarLeapMonth: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 1440,
    val showYears: Boolean = true,
    val showMonths: Boolean = true,
    val showDays: Boolean = true,
    val showHours: Boolean = true,
    val showMinutes: Boolean = true,
    val showSeconds: Boolean = true,
    val showSolarDate: Boolean = true,
    val showLunarDate: Boolean = true,
    val cardBackgroundColor: Int = 0xFFE9E3EC.toInt(),
    val cardTextColor: Int = 0xFF29232D.toInt(),
    val solarDisplayMask: Int = 63,
    val lunarDisplayMask: Int = 63,
    val countdownDisplayMask: Int = 63,
    val cardGradientId: String = "solid",
    val titleGradientId: String = "solid",
    val solarGradientId: String = "solid",
    val lunarGradientId: String = "solid",
    val countdownGradientId: String = "solid",
    val titleTextColor: Int = 0xFF29232D.toInt(),
    val solarTextColor: Int = 0xFF29232D.toInt(),
    val lunarTextColor: Int = 0xFF29232D.toInt(),
    val countdownTextColor: Int = 0xFF29232D.toInt(),
    val sortOrder: Int = Int.MAX_VALUE,
    val isPinned: Boolean = false
)
