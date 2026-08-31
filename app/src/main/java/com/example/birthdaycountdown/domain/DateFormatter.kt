package com.example.birthdaycountdown.domain

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class DateFormatPreference { CHINESE, NUMERIC }

object DateFormatter {
    private val chinese = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日 HH 时 mm 分 ss 秒")
    private val numeric = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    fun format(value: LocalDateTime, preference: DateFormatPreference): String = value.format(if (preference == DateFormatPreference.CHINESE) chinese else numeric)

    fun format(value: LocalDateTime, preference: DateFormatPreference, options: DisplayOptions): String {
        if (preference == DateFormatPreference.CHINESE) {
            return buildList {
                if (options.showYears) add("${value.year} 年")
                if (options.showMonths) add("${value.monthValue} 月")
                if (options.showDays) add("${value.dayOfMonth} 日")
                if (options.showHours) add("%02d 时".format(value.hour))
                if (options.showMinutes) add("%02d 分".format(value.minute))
                if (options.showSeconds) add("%02d 秒".format(value.second))
            }.joinToString(" ")
        }
        val date = buildList {
            if (options.showYears) add(value.year.toString())
            if (options.showMonths) add("%02d".format(value.monthValue))
            if (options.showDays) add("%02d".format(value.dayOfMonth))
        }.joinToString("/")
        val time = buildList {
            if (options.showHours) add("%02d".format(value.hour))
            if (options.showMinutes) add("%02d".format(value.minute))
            if (options.showSeconds) add("%02d".format(value.second))
        }.joinToString(":")
        return listOf(date, time).filter(String::isNotEmpty).joinToString(" ")
    }

    fun formatLunar(lunar: LunarDate, time: LocalTime, options: DisplayOptions): String = buildList {
        if (options.showYears) add(lunar.year.toString().map { chineseDigits[it.digitToInt()] }.joinToString("") + "年")
        if (options.showMonths) add((if (lunar.isLeapMonth) "闰" else "") + chineseMonth(lunar.month))
        if (options.showDays) add(chineseDay(lunar.day))
        if (options.showHours) add("%02d 时".format(time.hour))
        if (options.showMinutes) add("%02d 分".format(time.minute))
        if (options.showSeconds) add("%02d 秒".format(time.second))
    }.joinToString(" ")

    private val chineseDigits = listOf("〇", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    private fun chineseMonth(month: Int) = (if (month == 1) "正" else chineseNumber(month)) + "月"
    private fun chineseDay(day: Int) = when (day) {
        10 -> "初十"
        20 -> "二十"
        30 -> "三十"
        in 1..9 -> "初${chineseNumber(day)}"
        in 11..19 -> "十${chineseNumber(day - 10)}"
        in 21..29 -> "廿${chineseNumber(day - 20)}"
        else -> error("农历日期必须是 1-30")
    }
    private fun chineseNumber(value: Int) = when (value) {
        in 1..9 -> chineseDigits[value]
        in 10..19 -> "十" + if (value == 10) "" else chineseDigits[value - 10]
        else -> value.toString()
    }
}
