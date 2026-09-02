package com.example.birthdaycountdown.domain

import com.example.birthdaycountdown.data.WatchRecordEntity
import com.example.birthdaycountdown.data.SYSTEM_WATCHING_ID
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchlistRulesTest {
    @Test
    fun decrementClampsAtZero() {
        assertEquals(0, adjustedEpisode(currentEpisode = 0, delta = -1))
        assertEquals(6, adjustedEpisode(currentEpisode = 5, delta = 1))
    }

    @Test
    fun statusFilterKeepsOnlyRecordsInTheSelectedState() {
        assertEquals(true, matchesWatchStatus(SYSTEM_WATCHING_ID, SYSTEM_WATCHING_ID))
        assertEquals(false, matchesWatchStatus("COMPLETED", SYSTEM_WATCHING_ID))
    }

    @Test
    fun statusFilterSupportsCustomStatusIds() {
        assertEquals(true, matchesWatchStatus("CUSTOM_TO_WATCH", "CUSTOM_TO_WATCH"))
        assertEquals(false, matchesWatchStatus("CUSTOM_TO_WATCH", "CUSTOM_REWATCH"))
    }

    @Test
    fun resequencingOnlyChangesTheRecordsPassedFromTheVisibleArchive() {
        val records = listOf(
            WatchRecordEntity(id = 3, title = "三", categoryId = 1, currentEpisode = 1, status = "DROPPED", sortOrder = 8),
            WatchRecordEntity(id = 1, title = "一", categoryId = 1, currentEpisode = 1, status = "DROPPED", sortOrder = 4)
        )

        val reordered = resequenceWatchRecords(records)

        assertEquals(listOf(3L, 1L), reordered.map { it.id })
        assertEquals(listOf(0, 1), reordered.map { it.sortOrder })
        assertEquals(listOf("DROPPED", "DROPPED"), reordered.map { it.status })
    }
}
