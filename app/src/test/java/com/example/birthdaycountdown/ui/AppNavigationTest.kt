package com.example.birthdaycountdown.ui

import com.example.birthdaycountdown.data.RecordType
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationTest {
    @Test
    fun topLevelDestinationsUseTheApprovedOrder() {
        assertEquals(
            listOf(
                TopLevelDestination.TIME,
                TopLevelDestination.WATCHLIST,
                TopLevelDestination.AI,
                TopLevelDestination.PROFILE
            ),
            TOP_LEVEL_DESTINATIONS
        )
    }

    @Test
    fun recordEditRoutesCarryExistingIdsAndNewRecordTypes() {
        assertEquals("record/edit/42", recordEditRoute(recordId = 42L))
        assertEquals("record/new/BIRTHDAY", recordEditRoute(recordType = RecordType.BIRTHDAY))
    }
}
