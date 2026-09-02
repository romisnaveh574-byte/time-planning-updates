package com.example.birthdaycountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavSettingsTest {
    @Test
    fun iconCannotBeHiddenWhenLabelIsAlreadyHidden() {
        val item = BottomNavItemSettings("时间", BottomNavIconId.CLOCK, showIcon = true, showLabel = false)

        assertEquals(item, item.withIconVisibility(false))
    }

    @Test
    fun labelCannotBeHiddenWhenIconIsAlreadyHidden() {
        val item = BottomNavItemSettings("时间", BottomNavIconId.CLOCK, showIcon = false, showLabel = true)

        assertEquals(item, item.withLabelVisibility(false))
    }

    @Test
    fun iconCannotBeHiddenWhenLabelIsBlank() {
        val item = BottomNavItemSettings("", BottomNavIconId.CLOCK, showIcon = true, showLabel = true)

        assertEquals(item, item.withIconVisibility(false))
    }

    @Test
    fun legacyDefaultAddSlotBecomesWatchlistWithoutChangingPreferenceKeys() {
        val legacy = BottomNavSettings(
            add = BottomNavItemSettings("添加时间", BottomNavIconId.CALENDAR_PLUS, showIcon = true, showLabel = false)
        )

        val normalized = normalizeNavigationSettings(legacy)

        assertEquals("追剧", normalized.add.label)
        assertEquals(BottomNavIconId.MOVIE, normalized.add.icon)
        assertEquals(false, normalized.add.showLabel)
    }

    @Test
    fun defaultSecondNavigationItemRepresentsWatchlist() {
        assertEquals("追剧", DEFAULT_BOTTOM_NAV_SETTINGS.add.label)
        assertEquals(BottomNavIconId.MOVIE, DEFAULT_BOTTOM_NAV_SETTINGS.add.icon)
    }
}
