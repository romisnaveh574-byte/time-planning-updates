package com.example.birthdaycountdown.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DateFormatterTest {
    private val value = LocalDateTime.of(2026, 5, 23, 8, 35, 46)
    @Test fun chineseFormatIncludesAllUnits() = assertEquals("2026 年 5 月 23 日 08 时 35 分 46 秒", DateFormatter.format(value, DateFormatPreference.CHINESE))
    @Test fun numericFormatIsZeroPadded() = assertEquals("2026/05/23 08:35:46", DateFormatter.format(value, DateFormatPreference.NUMERIC))

    @Test fun selectedUnitsControlChineseDateText() {
        val options = DisplayOptions(showYears = true, showMonths = false, showDays = true, showHours = false, showMinutes = true, showSeconds = false)
        assertEquals("2026 年 23 日 35 分", DateFormatter.format(value, DateFormatPreference.CHINESE, options))
    }

    @Test fun selectedUnitsControlLunarDateText() {
        val options = DisplayOptions(showYears = true, showMonths = true, showDays = true, showHours = true, showMinutes = false, showSeconds = false)
        assertEquals("一九九九年 六月 初三 08 时", DateFormatter.formatLunar(LunarDate(1999, 6, 3), value.toLocalTime(), options))
    }
}
