package com.example.birthdaycountdown.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun roundTripPreservesLunarAndPinnedRecordFields() {
        val record = CountdownEntity(
            id = 42,
            type = RecordType.BIRTHDAY,
            name = "小明生日",
            dateTimeIso = "1999-07-15T08:35:46",
            calendarType = CalendarType.LUNAR,
            lunarYear = 1999,
            lunarMonth = 6,
            lunarDay = 3,
            lunarLeapMonth = false,
            isPinned = true,
            cardGradientId = "electric_blue_cyan"
        )

        val backup = AppBackup(listOf(record), emptyList(), emptyList())

        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup)))
    }

    @Test
    fun oldCountdownOnlyBackupDecodesWithEmptyWatchData() {
        val backup = BackupCodec.decode("""{"formatVersion":1,"records":[]}""")

        assertTrue(backup.watchCategories.isEmpty())
        assertTrue(backup.watchRecords.isEmpty())
    }

    @Test
    fun watchBackupRoundTrips() {
        val input = AppBackup(
            countdownRecords = emptyList(),
            watchCategories = listOf(WatchCategoryEntity(id = 7, name = "动漫", sortOrder = 0)),
            watchRecords = listOf(
                WatchRecordEntity(
                    id = 8,
                    title = "葬送的芙莉莲",
                    categoryId = 7,
                    currentEpisode = 12,
                    totalEpisodes = 28,
                    platform = "Bilibili",
                    status = WatchStatus.WATCHING,
                    lastWatchedAt = 1_700_000_000_000L,
                    sortOrder = 0
                )
            )
        )

        assertEquals(input, BackupCodec.decode(BackupCodec.encode(input)))
    }

    @Test
    fun v1WatchBackupMapsUnknownNonBlankStatusToWatching() {
        val backup = BackupCodec.decode(
            """
            {
              "formatVersion": 1,
              "records": [],
              "watchRecords": [{
                "id": 8,
                "title": "葬送的芙莉莲",
                "categoryId": 7,
                "currentEpisode": 12,
                "totalEpisodes": 28,
                "platform": "Bilibili",
                "status": "CUSTOM_STATUS",
                "lastWatchedAt": 1700000000000,
                "sortOrder": 0
              }]
            }
            """.trimIndent()
        )

        assertEquals(SYSTEM_WATCHING_ID, backup.watchRecords.single().status)
    }
}
