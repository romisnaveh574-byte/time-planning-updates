package com.example.birthdaycountdown.ui

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal object VisualTokens {
    const val primaryGradientStart: Int = 0xFF7047E8.toInt()
    const val primaryGradientEnd: Int = 0xFFD6297D.toInt()
    const val ink: Int = 0xFF1D1B2D.toInt()
}

internal enum class TaskTone { INFO, PROGRESS, SUCCESS, WARNING, ERROR }

internal fun taskToneFor(status: String): TaskTone = when (status.uppercase()) {
    "DONE", "COMPLETED", "SUCCESS" -> TaskTone.SUCCESS
    "FAILED", "ERROR" -> TaskTone.ERROR
    "WARNING" -> TaskTone.WARNING
    "PENDING", "QUEUED" -> TaskTone.INFO
    else -> TaskTone.PROGRESS
}

internal fun contrastRatio(first: Int, second: Int): Double {
    val firstLuminance = relativeLuminance(first)
    val secondLuminance = relativeLuminance(second)
    return (max(firstLuminance, secondLuminance) + 0.05) /
        (min(firstLuminance, secondLuminance) + 0.05)
}

internal fun minimumContrast(foregrounds: List<Int>, backgrounds: List<Int>): Double =
    foregrounds.flatMap { foreground -> backgrounds.map { background -> contrastRatio(foreground, background) } }
        .minOrNull() ?: 1.0

private fun relativeLuminance(argb: Int): Double {
    fun channel(shift: Int): Double {
        val value = ((argb shr shift) and 0xFF) / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}
