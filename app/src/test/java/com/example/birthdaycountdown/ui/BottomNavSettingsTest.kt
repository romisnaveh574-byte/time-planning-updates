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
}
