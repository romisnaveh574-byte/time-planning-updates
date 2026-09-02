package com.example.birthdaycountdown.domain

import com.example.birthdaycountdown.data.WatchRecordEntity

fun adjustedEpisode(currentEpisode: Int, delta: Int): Int =
    (currentEpisode + delta).coerceAtLeast(0)

fun matchesWatchStatus(recordStatus: String, selectedStatusId: String): Boolean =
    recordStatus == selectedStatusId

fun resequenceWatchRecords(records: List<WatchRecordEntity>): List<WatchRecordEntity> =
    records.mapIndexed { index, record -> record.copy(sortOrder = index) }
