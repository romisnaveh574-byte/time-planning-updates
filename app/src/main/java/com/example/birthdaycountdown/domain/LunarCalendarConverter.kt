package com.example.birthdaycountdown.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class LunarDate(val year: Int, val month: Int, val day: Int, val isLeapMonth: Boolean = false)

/** Converts Chinese lunar dates using the standard 1900-2099 lunisolar table. */
object LunarCalendarConverter {
    private const val FIRST_YEAR = 1900
    private val EPOCH = LocalDate.of(1900, 1, 31)
    private val YEAR_INFOS = intArrayOf(
        19416, 19168, 42352, 21717, 53856, 55632, 91476, 22176, 39632, 21970,
        19168, 42422, 42192, 53840, 119381, 46400, 54944, 44450, 38320, 84343,
        18800, 42160, 46261, 27216, 27968, 109396, 11104, 38256, 21234, 18800,
        25958, 54432, 59984, 28309, 23248, 11104, 100067, 37600, 116951, 51536,
        54432, 120998, 46416, 22176, 107956, 9680, 37584, 53938, 43344, 46423,
        27808, 46416, 86869, 19872, 42448, 83315, 21168, 43432, 59728, 27296,
        44710, 43856, 19296, 43748, 42352, 21088, 62051, 55632, 23383, 22176,
        38608, 19925, 19152, 42192, 54484, 53840, 54616, 46400, 46496, 103846,
        38320, 18864, 43380, 42160, 45690, 27216, 27968, 44870, 43872, 38256,
        19189, 18800, 25776, 29859, 59984, 27480, 23232, 43872, 38613, 37600,
        51552, 55636, 54432, 55888, 30034, 22176, 43959, 9680, 37584, 51893,
        43344, 46240, 47780, 44368, 21977, 19360, 42416, 86390, 21168, 43312,
        31060, 27296, 44368, 23378, 19296, 42726, 42208, 53856, 60005, 54576,
        23200, 30371, 38608, 19195, 19152, 42192, 118966, 53840, 54560, 56645,
        46496, 22224, 21938, 18864, 42359, 42160, 43600, 111189, 27936, 44448,
        84835, 37744, 18936, 18800, 25776, 92326, 59984, 27296, 108228, 43744,
        37600, 53987, 51552, 54615, 54432, 55888, 23893, 22176, 42704, 21972,
        21200, 43448, 43344, 46240, 46758, 44368, 21920, 43940, 42416, 21168,
        45683, 26928, 29495, 27296, 44368, 84821, 19296, 42352, 21732, 53600,
        59752, 54560, 55968, 92838, 22224, 19168, 43476, 41680, 53584, 62034
    )

    fun toSolar(lunar: LunarDate): LocalDate {
        require(lunar.year in FIRST_YEAR until FIRST_YEAR + YEAR_INFOS.size) { "农历年份需在 1900-2099 范围内" }
        require(lunar.month in 1..12) { "农历月份必须是 1-12" }
        val info = YEAR_INFOS[lunar.year - FIRST_YEAR]
        val leapMonth = info and 0xF
        if (lunar.isLeapMonth) require(leapMonth == lunar.month) { "该年份没有${lunar.month}月闰月" }
        var offset = (0 until lunar.year - FIRST_YEAR).sumOf(::yearDays)
        for (month in 1 until lunar.month) {
            offset += monthDays(info, month)
            if (month == leapMonth) offset += leapDays(info)
        }
        if (lunar.isLeapMonth) offset += monthDays(info, lunar.month)
        val maxDay = if (lunar.isLeapMonth) leapDays(info) else monthDays(info, lunar.month)
        require(lunar.day in 1..maxDay) { "农历日期超出该月范围" }
        return EPOCH.plusDays((offset + lunar.day - 1).toLong())
    }

    fun leapMonthForYear(year: Int): Int? {
        require(year in FIRST_YEAR until FIRST_YEAR + YEAR_INFOS.size) { "农历年份需在 1900-2099 范围内" }
        return (YEAR_INFOS[year - FIRST_YEAR] and 0xF).takeIf { it != 0 }
    }

    fun toLunar(solar: LocalDate): LunarDate {
        var offset = ChronoUnit.DAYS.between(EPOCH, solar).toInt()
        require(offset >= 0) { "公历日期需不早于 1900-01-31" }
        var yearIndex = 0
        while (yearIndex < YEAR_INFOS.size && offset >= yearDays(yearIndex)) {
            offset -= yearDays(yearIndex)
            yearIndex++
        }
        require(yearIndex < YEAR_INFOS.size) { "公历日期超出 2099 年范围" }
        val info = YEAR_INFOS[yearIndex]
        val leapMonth = info and 0xF
        for (month in 1..12) {
            val regularDays = monthDays(info, month)
            if (offset < regularDays) return LunarDate(FIRST_YEAR + yearIndex, month, offset + 1)
            offset -= regularDays
            if (month == leapMonth) {
                val leapDays = leapDays(info)
                if (offset < leapDays) return LunarDate(FIRST_YEAR + yearIndex, month, offset + 1, true)
                offset -= leapDays
            }
        }
        error("无法转换公历日期")
    }

    private fun yearDays(index: Int): Int {
        val info = YEAR_INFOS[index]
        return (1..12).sumOf { monthDays(info, it) } + if ((info and 0xF) != 0) leapDays(info) else 0
    }

    private fun monthDays(info: Int, month: Int): Int = if ((info and (0x10000 shr month)) != 0) 30 else 29

    private fun leapDays(info: Int): Int = if ((info and 0x10000) != 0) 30 else 29
}
