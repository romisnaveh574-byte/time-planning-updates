package com.example.birthdaycountdown.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalendarSelectionTest {
    @Test fun calendarTypeKeepsLunarLeapMonthFlag() {
        val value = LunarDate(2000, 6, 15, true)
        assertEquals(2000, value.year)
        assertEquals(6, value.month)
        assertEquals(15, value.day)
        assertEquals(true, value.isLeapMonth)
    }

    @Test fun lunar1999JuneThirdConvertsToJuly15() {
        assertEquals(LocalDate.of(1999, 7, 15), LunarCalendarConverter.toSolar(LunarDate(1999, 6, 3)))
    }

    @Test fun july15ConvertsBackToLunar1999JuneThird() {
        assertEquals(LunarDate(1999, 6, 3), LunarCalendarConverter.toLunar(LocalDate.of(1999, 7, 15)))
    }

    @Test fun year1999HasNoLeapMonth() {
        assertEquals(null, LunarCalendarConverter.leapMonthForYear(1999))
    }
}
