package com.example.birthdaycountdown.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDashboardTaskTest {
    @Test
    fun activeTaskProducesAProgressNotice() {
        val task = AiDashboardTask(1, 10, AiMode.CHAT.name, "问题", "PROCESSING", false, 100)

        assertEquals(AiDashboardNoticeState.ACTIVE, dashboardNoticeState(task))
    }

    @Test
    fun unseenCompletedTaskProducesACompletedNotice() {
        val task = AiDashboardTask(1, 10, AiMode.IMAGE.name, "图片", "DONE", false, 100)

        assertEquals(AiDashboardNoticeState.COMPLETED, dashboardNoticeState(task))
    }

    @Test
    fun viewedOrFailedTaskDoesNotProduceANotice() {
        val viewed = AiDashboardTask(1, 10, AiMode.CHAT.name, "问题", "DONE", true, 100)
        val failed = AiDashboardTask(2, 11, AiMode.IMAGE.name, "图片", "FAILED", false, 101)

        assertTrue(dashboardNoticeState(viewed) == null)
        assertTrue(dashboardNoticeState(failed) == null)
    }
}
