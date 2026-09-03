package com.example.birthdaycountdown.ui

import com.example.birthdaycountdown.data.CountdownEntity
import com.example.birthdaycountdown.data.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test

class CardColorPreferenceTest {
    @Test
    fun nonOverriddenRecordUsesGlobalColors() {
        val record = CountdownEntity(type = RecordType.BIRTHDAY, name = "A", dateTimeIso = "2026-01-01T00:00")
        val global = CardColors(1, 2, 3, 4, 5)
        assertEquals(global, effectiveCardColors(record, global))
    }

    @Test
    fun overriddenRecordUsesItsOwnColors() {
        val record = CountdownEntity(type = RecordType.BIRTHDAY, name = "A", dateTimeIso = "2026-01-01T00:00", useCustomCardColors = true, cardBackgroundColor = 11, titleTextColor = 12, solarTextColor = 13, lunarTextColor = 14, countdownTextColor = 15)
        assertEquals(CardColors(11, 12, 13, 14, 15), effectiveCardColors(record, CardColors(1, 2, 3, 4, 5)))
    }
}
