package com.example.birthdaycountdown.domain

import com.example.birthdaycountdown.data.WatchStatus
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
        assertEquals(true, matchesWatchStatus(WatchStatus.WATCHING, WatchStatus.WATCHING))
        assertEquals(false, matchesWatchStatus(WatchStatus.COMPLETED, WatchStatus.WATCHING))
    }
}
