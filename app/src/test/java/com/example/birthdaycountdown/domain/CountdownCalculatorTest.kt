package com.example.birthdaycountdown.domain

import com.example.birthdaycountdown.data.CountdownEntity
import com.example.birthdaycountdown.data.RecordType
import com.example.birthdaycountdown.data.CalendarType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.*

class CountdownCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test fun birthdayMovesToNextYearAfterTarget() {
        val record = CountdownEntity(type = RecordType.BIRTHDAY, name = "我", dateTimeIso = "2000-05-23T08:35:46")
        val now = ZonedDateTime.of(2026, 8, 26, 0, 0, 0, 0, zone)
        assertEquals(2027, CountdownCalculator.snapshot(record, now).target.year)
    }

    @Test fun futureAnniversaryIsCountdownOnly() {
        val record = CountdownEntity(type = RecordType.ANNIVERSARY, name = "未来", dateTimeIso = "2026-12-01T08:00")
        val now = ZonedDateTime.of(2026, 8, 26, 0, 0, 0, 0, zone)
        val snapshot = CountdownCalculator.snapshot(record, now)
        assertTrue(snapshot.countdown != null)
        assertEquals(null, snapshot.elapsed)
    }

    @Test fun leapBirthdayUsesFebruary28InNonLeapYear() {
        val record = CountdownEntity(type = RecordType.BIRTHDAY, name = "闰日", dateTimeIso = "2000-02-29T08:00")
        val now = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, zone)
        assertEquals(LocalDate.of(2026, 2, 28), CountdownCalculator.snapshot(record, now).target.toLocalDate())
    }

    @Test fun invalidSavedLeapMonthFallsBackToRegularMonth() {
        val record = CountdownEntity(
            type = RecordType.ANNIVERSARY,
            name = "旧记录",
            dateTimeIso = "1999-07-15T08:00",
            calendarType = CalendarType.LUNAR,
            lunarYear = 1999,
            lunarMonth = 6,
            lunarDay = 3,
            lunarLeapMonth = true
        )

        assertEquals(LocalDate.of(1999, 7, 15), CountdownCalculator.solarDateTime(record).toLocalDate())
    }
}
