package com.example.birthdaycountdown.domain

import com.example.birthdaycountdown.data.WatchStatus
import com.example.birthdaycountdown.data.WatchRecordEntity

fun adjustedEpisode(currentEpisode: Int, delta: Int): Int =
    (currentEpisode + delta).coerceAtLeast(0)

fun matchesWatchStatus(recordStatus: String, selectedStatus: WatchStatus): Boolean =
    recordStatus == selectedStatus.name

fun initialWatchStatus(recordStatus: String?, requestedStatus: WatchStatus): WatchStatus =
    WatchStatus.entries.firstOrNull { it.name == recordStatus } ?: requestedStatus

fun resequenceWatchRecords(records: List<WatchRecordEntity>): List<WatchRecordEntity> =
    records.mapIndexed { index, record -> record.copy(sortOrder = index) }
