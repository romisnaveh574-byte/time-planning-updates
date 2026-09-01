package com.example.birthdaycountdown.domain

import com.example.birthdaycountdown.data.WatchStatus

fun adjustedEpisode(currentEpisode: Int, delta: Int): Int =
    (currentEpisode + delta).coerceAtLeast(0)

fun matchesWatchStatus(recordStatus: WatchStatus, selectedStatus: WatchStatus): Boolean =
    recordStatus == selectedStatus
