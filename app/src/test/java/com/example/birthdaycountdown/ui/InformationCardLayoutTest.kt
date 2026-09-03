package com.example.birthdaycountdown.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InformationCardLayoutTest {
    @Test
    fun stacksHeaderOnNarrowScreensOrLargeFonts() {
        assertTrue(shouldStackInformationCard(340, 1f))
        assertTrue(shouldStackInformationCard(400, 1.3f))
        assertFalse(shouldStackInformationCard(400, 1f))
    }
}
