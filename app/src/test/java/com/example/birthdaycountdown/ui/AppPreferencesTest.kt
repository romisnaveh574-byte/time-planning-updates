package com.example.birthdaycountdown.ui

import com.example.birthdaycountdown.domain.DateFormatPreference
import org.junit.Assert.assertEquals
import org.junit.Test

class AppPreferencesTest {
    @Test
    fun invalidDateFormatFallsBackToChinese() {
        assertEquals(DateFormatPreference.CHINESE, parseDateFormatPreference(null))
        assertEquals(DateFormatPreference.CHINESE, parseDateFormatPreference("BROKEN"))
        assertEquals(DateFormatPreference.NUMERIC, parseDateFormatPreference("NUMERIC"))
    }
}
