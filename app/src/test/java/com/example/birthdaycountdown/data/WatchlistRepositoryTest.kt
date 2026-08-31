package com.example.birthdaycountdown.data

import org.junit.Assert.assertThrows
import org.junit.Test

class WatchlistRepositoryTest {
    @Test
    fun recordRejectsBlankTitle() {
        assertThrows(IllegalArgumentException::class.java) {
            WatchRecordEntity(title = " ", categoryId = 1, currentEpisode = 0)
        }
    }

    @Test
    fun recordRejectsNegativeEpisode() {
        assertThrows(IllegalArgumentException::class.java) {
            WatchRecordEntity(title = "海贼王", categoryId = 1, currentEpisode = -1)
        }
    }
}
