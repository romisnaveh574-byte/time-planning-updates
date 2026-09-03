package com.example.birthdaycountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CardLayoutStyleTest {
    @Test
    fun exposesThreeSelectableLayouts() {
        assertEquals(listOf("标准版", "紧凑版", "侧栏版"), cardLayoutStyleLabels())
    }

    @Test
    fun unknownStoredLayoutFallsBackToStandard() {
        assertEquals(CardLayoutStyle.STANDARD, parseCardLayoutStyle("missing"))
    }
}
