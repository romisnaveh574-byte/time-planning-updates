package com.example.birthdaycountdown.domain

import com.example.birthdaycountdown.data.CountdownEntity
import com.example.birthdaycountdown.data.RecordType
import com.example.birthdaycountdown.data.CalendarType
import java.time.*

data class TimerSnapshot(
    val countdown: Duration? = null,
    val elapsed: Period? = null,
    val elapsedRemainder: Duration? = null,
    val nextAnniversary: Duration? = null,
    val target: ZonedDateTime
)

object CountdownCalculator {
    fun snapshot(record: CountdownEntity, now: ZonedDateTime): TimerSnapshot {
        val original = solarDateTime(record)
        val zoned = original.atZone(now.zone)
        return if (record.type == RecordType.BIRTHDAY) {
            val target = nextBirthday(record, zoned, now)
            TimerSnapshot(countdown = Duration.between(now, target), target = target)
        } else {
            if (zoned.isAfter(now)) {
                TimerSnapshot(countdown = Duration.between(now, zoned), target = zoned)
            } else {
                val elapsed = calendarElapsed(zoned, now)
                val next = nextAnniversary(record, zoned, now)
                TimerSnapshot(
                    elapsed = elapsed.first,
                    elapsedRemainder = elapsed.second,
                    nextAnniversary = Duration.between(now, next),
                    target = next
                )
            }
        }
    }

    fun solarDateTime(record: CountdownEntity): LocalDateTime {
        if (record.calendarType == CalendarType.LUNAR && record.lunarYear != null && record.lunarMonth != null && record.lunarDay != null) {
            val requested = LunarDate(record.lunarYear, record.lunarMonth, record.lunarDay, record.lunarLeapMonth)
            val solar = runCatching { LunarCalendarConverter.toSolar(requested) }
                .getOrElse { LunarCalendarConverter.toSolar(requested.copy(isLeapMonth = false)) }
            return LocalDateTime.of(solar, LocalDateTime.parse(record.dateTimeIso).toLocalTime())
        }
        return LocalDateTime.parse(record.dateTimeIso)
    }

    private fun nextBirthday(record: CountdownEntity, original: ZonedDateTime, now: ZonedDateTime): ZonedDateTime {
        var year = now.year
        var date = annualDate(record, original.toLocalDate(), year)
        var target = ZonedDateTime.of(date, original.toLocalTime(), now.zone)
        if (!target.isAfter(now)) {
            year += 1
            date = annualDate(record, original.toLocalDate(), year)
            target = ZonedDateTime.of(date, original.toLocalTime(), now.zone)
        }
        return target
    }

    private fun nextAnniversary(record: CountdownEntity, original: ZonedDateTime, now: ZonedDateTime): ZonedDateTime {
        var target = ZonedDateTime.of(annualDate(record, original.toLocalDate(), now.year), original.toLocalTime(), now.zone)
        if (!target.isAfter(now)) target = ZonedDateTime.of(annualDate(record, original.toLocalDate(), now.year + 1), original.toLocalTime(), now.zone)
        return target
    }

    private fun annualDate(record: CountdownEntity, fallback: LocalDate, year: Int): LocalDate {
        if (record.calendarType != CalendarType.LUNAR || record.lunarMonth == null || record.lunarDay == null) return birthdayDate(fallback, year)
        val requested = LunarDate(year, record.lunarMonth, record.lunarDay, record.lunarLeapMonth)
        return runCatching { LunarCalendarConverter.toSolar(requested) }.getOrElse {
            LunarCalendarConverter.toSolar(requested.copy(isLeapMonth = false))
        }
    }

    private fun birthdayDate(date: LocalDate, year: Int): LocalDate =
        if (date.monthValue == 2 && date.dayOfMonth == 29 && !Year.isLeap(year.toLong())) LocalDate.of(year, 2, 28)
        else date.withYear(year)

    private fun calendarElapsed(start: ZonedDateTime, now: ZonedDateTime): Pair<Period, Duration> {
        var cursor = start.toLocalDateTime()
        var years = 0
        while (cursor.plusYears(1).isBefore(now.toLocalDateTime()) || cursor.plusYears(1).isEqual(now.toLocalDateTime())) { cursor = cursor.plusYears(1); years++ }
        var months = 0
        while (cursor.plusMonths(1).isBefore(now.toLocalDateTime()) || cursor.plusMonths(1).isEqual(now.toLocalDateTime())) { cursor = cursor.plusMonths(1); months++ }
        var days = 0
        while (cursor.plusDays(1).isBefore(now.toLocalDateTime()) || cursor.plusDays(1).isEqual(now.toLocalDateTime())) { cursor = cursor.plusDays(1); days++ }
        val remainder = Duration.between(cursor.atZone(now.zone), now)
        return Period.of(years, months, days) to remainder
    }

    fun durationParts(duration: Duration): Triple<Long, Long, Long> {
        val seconds = duration.seconds.coerceAtLeast(0)
        return Triple(seconds / 86400, (seconds % 86400) / 3600, (seconds % 3600) / 60)
    }

    fun secondsPart(duration: Duration): Long = duration.seconds.coerceAtLeast(0) % 60
}
