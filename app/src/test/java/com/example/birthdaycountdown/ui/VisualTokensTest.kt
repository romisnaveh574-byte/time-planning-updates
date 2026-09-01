package com.example.birthdaycountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualTokensTest {
    @Test
    fun primaryGradientSupportsWhiteTextAcrossBothEnds() {
        assertTrue(contrastRatio(0xFFFFFFFF.toInt(), VisualTokens.primaryGradientStart) >= 4.5)
        assertTrue(contrastRatio(0xFFFFFFFF.toInt(), VisualTokens.primaryGradientEnd) >= 4.5)
    }

    @Test
    fun taskStatesMapToSemanticLabels() {
        assertEquals(TaskTone.PROGRESS, taskToneFor("RUNNING"))
        assertEquals(TaskTone.SUCCESS, taskToneFor("DONE"))
        assertEquals(TaskTone.ERROR, taskToneFor("FAILED"))
        assertEquals(TaskTone.INFO, taskToneFor("QUEUED"))
    }

    @Test
    fun minimumContrastChecksEveryForegroundAndBackgroundColor() {
        val result = minimumContrast(
            foregrounds = listOf(0xFFFFFFFF.toInt(), 0xFF1D1B2D.toInt()),
            backgrounds = listOf(0xFFFFFFFF.toInt(), 0xFF7047E8.toInt())
        )

        assertEquals(1.0, result, 0.001)
    }
}
