package com.example.birthdaycountdown.domain

import com.example.birthdaycountdown.data.WatchStatus
import com.example.birthdaycountdown.data.WatchRecordEntity

fun adjustedEpisode(currentEpisode: Int, delta: Int): Int =
    (currentEpisode + delta).coerceAtLeast(0)

fun matchesWatchStatus(recordStatus: WatchStatus, selectedStatus: WatchStatus): Boolean =
    recordStatus == selectedStatus

fun initialWatchStatus(recordStatus: WatchStatus?, requestedStatus: WatchStatus): WatchStatus =
    recordStatus ?: requestedStatus

fun resequenceWatchRecords(records: List<WatchRecordEntity>): List<WatchRecordEntity> =
    records.mapIndexed { index, record -> record.copy(sortOrder = index) }
