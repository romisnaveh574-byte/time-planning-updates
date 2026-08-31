package com.example.birthdaycountdown.domain

import java.time.Duration
import java.time.Period

data class DisplayOptions(
    val showYears: Boolean = true,
    val showMonths: Boolean = true,
    val showDays: Boolean = true,
    val showHours: Boolean = true,
    val showMinutes: Boolean = true,
    val showSeconds: Boolean = true
)

object DisplayFormatter {
    fun elapsed(period: Period, remainder: Duration, options: DisplayOptions): String {
        val parts = mutableListOf<String>()
        if (options.showYears) parts += "${period.years} 年"
        if (options.showMonths) parts += "${period.months} 个月"
        if (options.showDays) parts += "${period.days} 天"
        appendDuration(parts, remainder, options)
        return parts.ifEmpty { listOf("未选择显示单位") }.joinToString(" ")
    }

    fun countdown(duration: Duration, options: DisplayOptions): String {
        val totalSeconds = duration.seconds.coerceAtLeast(0)
        val parts = mutableListOf<String>()
        if (options.showDays) parts += "${totalSeconds / 86400} 天"
        if (options.showHours) parts += "${(totalSeconds % 86400) / 3600} 时"
        if (options.showMinutes) parts += "${(totalSeconds % 3600) / 60} 分"
        if (options.showSeconds) parts += "${totalSeconds % 60} 秒"
        return if (parts.isEmpty()) "未选择显示单位" else parts.joinToString(" ")
    }

    private fun appendDuration(parts: MutableList<String>, remainder: Duration, options: DisplayOptions) {
        val seconds = remainder.seconds.coerceAtLeast(0)
        if (options.showHours) parts += "${seconds / 3600} 时"
        if (options.showMinutes) parts += "${(seconds % 3600) / 60} 分"
        if (options.showSeconds) parts += "${seconds % 60} 秒"
    }
}
