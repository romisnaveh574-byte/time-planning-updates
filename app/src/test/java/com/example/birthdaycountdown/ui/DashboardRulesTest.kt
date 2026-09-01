package com.example.birthdaycountdown.ui

import com.example.birthdaycountdown.data.RecordType
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardRulesTest {
    @Test
    fun showsBirthdayAndAnniversaryWhenBothAreWithinSevenDays() {
        val selected = selectDashboardReminders(
            listOf(
                DashboardReminderCandidate(1, RecordType.BIRTHDAY, Duration.ofDays(3)),
                DashboardReminderCandidate(2, RecordType.BIRTHDAY, Duration.ofDays(5)),
                DashboardReminderCandidate(3, RecordType.ANNIVERSARY, Duration.ofDays(6))
            )
        )

        assertEquals(listOf(1L, 3L), selected.map { it.recordId })
    }

    @Test
    fun showsOnlyTheNearTypeWhenTheOtherTypeIsOutsideSevenDays() {
        val selected = selectDashboardReminders(
            listOf(
                DashboardReminderCandidate(1, RecordType.BIRTHDAY, Duration.ofDays(2)),
                DashboardReminderCandidate(2, RecordType.ANNIVERSARY, Duration.ofDays(10))
            )
        )

        assertEquals(listOf(1L), selected.map { it.recordId })
    }

    @Test
    fun showsTheSingleNearestRecordWhenNothingIsWithinSevenDays() {
        val selected = selectDashboardReminders(
            listOf(
                DashboardReminderCandidate(1, RecordType.BIRTHDAY, Duration.ofDays(20)),
                DashboardReminderCandidate(2, RecordType.ANNIVERSARY, Duration.ofDays(12))
            )
        )

        assertEquals(listOf(2L), selected.map { it.recordId })
    }
}
