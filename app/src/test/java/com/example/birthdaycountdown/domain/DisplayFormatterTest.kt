package com.example.birthdaycountdown.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Period

class DisplayFormatterTest {
    @Test fun hiddenDaysAreNotDuplicatedInElapsedText() {
        val options = DisplayOptions(showYears = true, showMonths = true, showDays = true, showHours = true, showMinutes = true, showSeconds = false)
        assertEquals("1 年 2 个月 3 天 04 时 05 分", DisplayFormatter.elapsed(Period.of(1, 2, 3), Duration.ofHours(4).plusMinutes(5), options))
    }

    @Test fun countdownUsesOnlySelectedUnits() {
        val options = DisplayOptions(showYears = false, showMonths = false, showDays = true, showHours = false, showMinutes = true, showSeconds = true)
        assertEquals("3 天 04 分 05 秒", DisplayFormatter.countdown(Duration.ofDays(3).plusHours(2).plusMinutes(4).plusSeconds(5), options))
    }
}
