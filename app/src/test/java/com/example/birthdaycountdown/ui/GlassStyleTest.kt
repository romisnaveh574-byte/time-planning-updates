package com.example.birthdaycountdown.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class GlassStyleTest {
    @Test
    fun glassPanelsKeepTheirBackgroundVisibleWithAHighlightAndElevation() {
        assertTrue(GlassStyle.panelAlpha < 1f)
        assertTrue(GlassStyle.highlightAlpha > 0f)
        assertTrue(GlassStyle.elevation > 0f)
    }
}
