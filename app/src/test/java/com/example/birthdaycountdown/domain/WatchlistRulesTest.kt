package com.example.birthdaycountdown.domain

import com.example.birthdaycountdown.data.WatchStatus
import com.example.birthdaycountdown.data.WatchRecordEntity
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
        assertEquals(true, matchesWatchStatus(WatchStatus.WATCHING.name, WatchStatus.WATCHING))
        assertEquals(false, matchesWatchStatus(WatchStatus.COMPLETED.name, WatchStatus.WATCHING))
    }

    @Test
    fun newRecordUsesTheCurrentlySelectedArchiveStatus() {
        listOf(WatchStatus.COMPLETED, WatchStatus.PAUSED, WatchStatus.DROPPED, WatchStatus.ARCHIVED).forEach { status ->
            assertEquals(status, initialWatchStatus(recordStatus = null, requestedStatus = status))
        }
        assertEquals(
            WatchStatus.COMPLETED,
            initialWatchStatus(recordStatus = WatchStatus.COMPLETED.name, requestedStatus = WatchStatus.DROPPED)
        )
    }

    @Test
    fun resequencingOnlyChangesTheRecordsPassedFromTheVisibleArchive() {
        val records = listOf(
            WatchRecordEntity(id = 3, title = "三", categoryId = 1, currentEpisode = 1, status = WatchStatus.DROPPED, sortOrder = 8),
            WatchRecordEntity(id = 1, title = "一", categoryId = 1, currentEpisode = 1, status = WatchStatus.DROPPED, sortOrder = 4)
        )

        val reordered = resequenceWatchRecords(records)

        assertEquals(listOf(3L, 1L), reordered.map { it.id })
        assertEquals(listOf(0, 1), reordered.map { it.sortOrder })
        assertEquals(listOf(WatchStatus.DROPPED.name, WatchStatus.DROPPED.name), reordered.map { it.status })
    }
}
