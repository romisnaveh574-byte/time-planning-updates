package com.example.birthdaycountdown.ui

import com.example.birthdaycountdown.data.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddFlowRulesTest {
    @Test
    fun birthdayChoicePreselectsBirthdayEditor() {
        assertEquals(RecordType.BIRTHDAY, AddChoice.BIRTHDAY.recordType)
    }

    @Test
    fun anniversaryChoicePreselectsAnniversaryEditor() {
        assertEquals(RecordType.ANNIVERSARY, AddChoice.ANNIVERSARY.recordType)
    }

    @Test
    fun watchlistChoiceDoesNotOpenCountdownEditor() {
        assertNull(AddChoice.WATCHLIST.recordType)
    }

    @Test
    fun watchlistSummaryUsesTheCurrentRecordCount() {
        assertEquals("正在追 5 部", watchlistSummary(5))
    }
}
