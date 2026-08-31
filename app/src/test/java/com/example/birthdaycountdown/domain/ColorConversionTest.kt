package com.example.birthdaycountdown.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorConversionTest {
    @Test
    fun blackRoundTrips() {
        assertEquals(CmykColor(0, 0, 0, 100), rgbToCmyk(RgbColor(0, 0, 0)))
        assertEquals(RgbColor(0, 0, 0), cmykToRgb(CmykColor(0, 0, 0, 100)))
    }

    @Test
    fun redRoundTrips() {
        assertEquals(CmykColor(0, 100, 100, 0), rgbToCmyk(RgbColor(255, 0, 0)))
        assertEquals(RgbColor(255, 0, 0), cmykToRgb(CmykColor(0, 100, 100, 0)))
    }
}
