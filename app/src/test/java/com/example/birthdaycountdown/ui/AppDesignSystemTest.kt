package com.example.birthdaycountdown.ui

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
}
