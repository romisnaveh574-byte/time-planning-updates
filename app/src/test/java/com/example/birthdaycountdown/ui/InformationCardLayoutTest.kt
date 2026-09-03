package com.example.birthdaycountdown.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InformationCardLayoutTest {
    @Test
    fun stacksHeaderOnNarrowScreensOrLargeFonts() {
        assertTrue(shouldStackInformationCard(340, 1f))
        assertTrue(shouldStackInformationCard(400, 1.3f))
        assertFalse(shouldStackInformationCard(400, 1f))
    }

    @Test
    fun informationCardsUseGradientSurface() {
        assertTrue(informationCardGradientColors("purple_pink", 0xFF7047E8.toInt()).size >= 2)
        assertTrue(informationCardGradientColors("missing", 0xFF7047E8.toInt()).size >= 2)
    }

    @Test
    fun cardStyleEditorOnlyExposesBackgroundGradient() {
        assertEquals(listOf("卡片"), cardStyleEditorTargets())
    }
}
