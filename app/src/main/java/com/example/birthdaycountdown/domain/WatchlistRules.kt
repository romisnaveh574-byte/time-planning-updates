package com.example.birthdaycountdown.domain

fun adjustedEpisode(currentEpisode: Int, delta: Int): Int =
    (currentEpisode + delta).coerceAtLeast(0)
