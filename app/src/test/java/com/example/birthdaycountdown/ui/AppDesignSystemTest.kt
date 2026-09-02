package com.example.birthdaycountdown.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDesignSystemTest {
    @Test
    fun cleanEfficiencyTokensUseCompactStableDimensions() {
        assertEquals(8.dp, AppUiTokens.surfaceCornerRadius)
        assertEquals(16.dp, AppUiTokens.pageHorizontalPadding)
        assertEquals(48.dp, AppUiTokens.minimumTouchTarget)
    }

    @Test
    fun cleanEfficiencyLargeSurfacesStayOnEightDpCorners() {
        assertEquals(8.dp, AppUiTokens.largeSurfaceCornerRadius)
    }

    @Test
    fun cleanEfficiencyWarningPaletteUsesExplicitLightAndDarkColors() {
        assertEquals(
            StatusColors(container = Color(0xFFFFE2B8), content = Color(0xFF5C3B00)),
            warningStatusColors(useDarkTheme = false)
        )
        assertEquals(
            StatusColors(container = Color(0xFF6A4A0A), content = Color(0xFFFFE2B8)),
            warningStatusColors(useDarkTheme = true)
        )
    }
}
