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

    @Test
    fun invalidLatestReleaseIsReportedAsFailureInsteadOfLatest() {
        assertEquals(
            UpdateCheckResult.Failed("更新信息格式无效"),
            classifyUpdateResult(AppVersion(26, "1.8.1"), null)
        )
    }
}
