package com.example.birthdaycountdown.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseLinksTest {
    @Test
    fun latestReleasePageUsesTheConfiguredRepository() {
        assertEquals(
            "https://github.com/example/time-planning/releases/latest",
            latestReleasePageUrl("example", "time-planning")
        )
    }
}
