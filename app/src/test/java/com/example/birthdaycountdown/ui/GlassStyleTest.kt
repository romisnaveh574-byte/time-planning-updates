package com.example.birthdaycountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassStyleTest {
    @Test
    fun panelsUseTheCompactOpaqueSurfaceStyle() {
        assertEquals(1f, GlassStyle.panelAlpha)
        assertEquals(8, GlassStyle.surfaceCornerRadius.value)
        assertTrue(GlassStyle.primaryGradient.isNotEmpty())
        assertTrue(GlassStyle.elevation > 0f)
    }
}
