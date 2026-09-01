package com.example.birthdaycountdown.ui

import com.example.birthdaycountdown.data.RecordType
import java.time.Duration

internal data class DashboardReminderCandidate(
    val recordId: Long,
    val type: RecordType,
    val duration: Duration
)

internal fun selectDashboardReminders(
    candidates: List<DashboardReminderCandidate>,
    threshold: Duration = Duration.ofDays(7)
): List<DashboardReminderCandidate> {
    val nearestByType = RecordType.entries.mapNotNull { type ->
        candidates.filter { it.type == type }.minByOrNull { it.duration }
    }
    val withinThreshold = nearestByType.filter { it.duration <= threshold }.sortedBy { it.duration }
    return withinThreshold.ifEmpty { nearestByType.minByOrNull { it.duration }?.let(::listOf).orEmpty() }
}
