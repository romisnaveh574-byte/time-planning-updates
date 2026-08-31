package com.example.birthdaycountdown.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AiLogicTest {
    @Test
    fun normalizeEndpointAcceptsModelPlazaAndV1Variants() {
        assertEquals("https://wawapii.com/v1", normalizeAiBaseUrl("https://wawapii.com/model-plaza"))
        assertEquals("https://wawapii.com/v1", normalizeAiBaseUrl("https://wawapii.com/v1/"))
        assertEquals("https://host.example/api/v1", normalizeAiBaseUrl("https://host.example/api/v1"))
    }

    @Test
    fun classifyHttpErrorsProvidesActionableChineseMessages() {
        assertTrue(classifyAiHttpError(401, "").contains("API Key"))
        assertTrue(classifyAiHttpError(404, "").contains("接口地址"))
        assertTrue(classifyAiHttpError(500, "服务异常").contains("服务异常"))
    }

    @Test
    fun conversationRequestIncludesPreviousMessages() {
        val messages = buildChatMessages(
            listOf(ChatTurn("user", "你好"), ChatTurn("assistant", "你好，有什么可以帮你？")),
            "继续刚才的话题"
        )
        assertEquals(3, messages.size)
        assertEquals("assistant", messages[1].role)
        assertEquals("继续刚才的话题", messages[2].text)
    }

    @Test
    fun imagePayloadRejectsOversizedFiles() {
        assertThrows(IllegalArgumentException::class.java) { validateAiImageSize(11L * 1024 * 1024) }
    }

    @Test
    fun imageSizeUsesRequestedLongestEdgeAndAllowsModelDefaultRatio() {
        assertEquals(null, imageSizeFor("1K", "原比例"))
        assertEquals("2048x1152", imageSizeFor("2K", "原比例", 4000, 2250))
        assertEquals("1365x2048", imageSizeFor("2K", "2:3"))
        assertEquals("2048x1365", imageSizeFor("2K", "3:2"))
        assertEquals("3072x4096", imageSizeFor("4K", "3:4"))
        assertEquals("4096x2304", imageSizeFor("4K", "16:9"))
    }
}
