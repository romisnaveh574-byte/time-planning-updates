package com.example.birthdaycountdown.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchlistRulesTest {
    @Test
    fun decrementClampsAtZero() {
        assertEquals(0, adjustedEpisode(currentEpisode = 0, delta = -1))
        assertEquals(6, adjustedEpisode(currentEpisode = 5, delta = 1))
    }
}
