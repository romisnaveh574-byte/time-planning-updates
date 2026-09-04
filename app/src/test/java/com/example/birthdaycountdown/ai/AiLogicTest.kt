package com.example.birthdaycountdown.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import com.example.birthdaycountdown.notifications.nextReminderReference
import com.example.birthdaycountdown.ui.markdownDisplayText
import java.net.SocketException

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
    fun explainsUnavailableImageProviderAccountsInChinese() {
        assertTrue(classifyAiHttpError(503, "No available compatible accounts").contains("上游账号"))
    }

    @Test
    fun retriesChatSynchronouslyOnlyWhenStreamFailsBeforeAnyReply() {
        val error = SocketException("Software caused connection abort")
        assertTrue(shouldFallbackToSyncChat(error, hasReceivedReply = false))
        assertEquals(false, shouldFallbackToSyncChat(error, hasReceivedReply = true))
    }

    @Test
    fun convertsNetworkAbortIntoAnActionableMessage() {
        assertTrue(aiFailureMessage(SocketException("Software caused connection abort"), "回复失败").contains("网络连接"))
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

    @Test
    fun reportsProviderSizeDegradation() {
        assertEquals("2048x2048", compareImageSize("2048x2048", ImageOutputInfo(1254, 1254)))
        assertEquals(null, compareImageSize("2048x2048", ImageOutputInfo(2048, 2048)))
    }

    @Test
    fun detectsReferenceImageMimeTypeFromFileName() {
        assertEquals("image/png", referenceImageMimeType("reference.PNG"))
        assertEquals("image/webp", referenceImageMimeType("reference.webp"))
        assertEquals("image/jpeg", referenceImageMimeType("reference.jpg"))
    }

    @Test
    fun preservesSupportedReferenceImageExtensions() {
        assertEquals("png", referenceImageExtension("image/png"))
        assertEquals("webp", referenceImageExtension("image/webp"))
        assertEquals("jpg", referenceImageExtension("image/jpeg"))
    }

    @Test
    fun onlyTreatsKnownAsyncNotFoundResponseAsUnsupported() {
        assertTrue(isAsyncImageUnsupported(404, "async image tasks are not enabled"))
        assertTrue(isAsyncImageUnsupported(404, "not_found_error"))
        assertEquals(false, isAsyncImageUnsupported(404, "接口地址或路径不存在，请检查中转站地址"))
    }

    @Test
    fun acceptsCompletedPayloadReturnedByAsyncImageEndpoint() {
        assertTrue(hasCompletedImagePayload(JSONObject("{\"data\":[{\"url\":\"https://example.com/image.png\"}]}")))
        assertTrue(hasCompletedImagePayload(JSONObject("{\"b64_json\":\"abc\"}")))
        assertEquals(false, hasCompletedImagePayload(JSONObject("{\"task_id\":\"task-1\"}")))
    }

    @Test
    fun identifiesImageGenerationStagesAsActive() {
        assertTrue(isActiveAiStatus("SUBMITTING"))
        assertTrue(isActiveAiStatus("QUEUED"))
        assertTrue(isActiveAiStatus("PROCESSING"))
        assertTrue(isActiveAiStatus("SAVING"))
        assertEquals(false, isActiveAiStatus("DONE"))
        assertEquals(false, isActiveAiStatus("FAILED"))
    }

    @Test
    fun recognizesInterruptedImageStagesAsRecoverable() {
        assertTrue(isRecoverableAiStatus("PENDING"))
        assertTrue(isRecoverableAiStatus("PROCESSING"))
        assertTrue(isRecoverableAiStatus("SAVING"))
        assertEquals(false, isRecoverableAiStatus("DONE"))
        assertEquals(false, isRecoverableAiStatus("FAILED"))
    }

    @Test
    fun onlyShowsAiSetupHintForIncompleteEndpoint() {
        assertEquals(false, needsAiSetup(AiEndpointConfig("https://relay.example/v1", "key", "model")))
        assertTrue(needsAiSetup(AiEndpointConfig("https://relay.example/v1", "", "model")))
    }

    @Test
    fun nextReminderReferenceMovesPastTheCurrentOccurrence() {
        val now = java.time.ZonedDateTime.parse("2026-09-01T08:00:00+08:00[Asia/Shanghai]")
        assertEquals(now.plusYears(1), nextReminderReference(now))
    }

    @Test
    fun labelsImageGenerationStagesWithoutFakePercentages() {
        assertEquals("正在提交", imageGenerationStatusLabel("SUBMITTING"))
        assertEquals("排队中", imageGenerationStatusLabel("QUEUED"))
        assertEquals("正在生成", imageGenerationStatusLabel("PROCESSING"))
        assertEquals("正在保存", imageGenerationStatusLabel("SAVING"))
    }

    @Test
    fun labelsAiHistoryByConversationType() {
        assertEquals("AI 对话", aiHistoryModeLabel("CHAT"))
        assertEquals("AI 生图", aiHistoryModeLabel("IMAGE"))
    }

    @Test
    fun labelsGallerySaveOutcome() {
        assertEquals("已保存到相册", gallerySaveResultLabel(true))
        assertEquals("保存到相册失败", gallerySaveResultLabel(false))
    }

    @Test
    fun selectsAndRemovesProviderProfilesWithoutRemovingTheLastOne() {
        val first = AiProviderProfile("1", "主站", AiEndpointConfig(model = "chat-a"))
        val second = AiProviderProfile("2", "备用", AiEndpointConfig(model = "chat-b"))
        assertEquals(second, selectedProvider(listOf(first, second), "2"))
        assertEquals(listOf(first), removeProvider(listOf(first, second), "2"))
        assertEquals(listOf(first), removeProvider(listOf(first), "1"))
    }

    @Test
    fun filtersModelRecommendationsByUseCaseWithoutHidingUnknownProviders() {
        val models = listOf("gpt-5", "flux-pro", "dall-e-3", "custom-model")
        assertEquals(listOf("flux-pro", "dall-e-3"), filterAiModels(models, image = true))
        assertEquals(listOf("gpt-5", "custom-model"), filterAiModels(models, image = false))
        assertEquals(listOf("custom-model"), filterAiModels(listOf("custom-model"), image = true))
    }

    @Test
    fun acceptsStringAndStandardObjectChatStreamPayloads() {
        assertEquals("普通字符串", parseChatStreamPayload("\"普通字符串\""))
        assertEquals("纯文本片段", parseChatStreamPayload("纯文本片段"))
        assertEquals(
            "标准片段",
            parseChatStreamPayload("""{"choices":[{"delta":{"content":"标准片段"}}]}""")
        )
    }

    @Test
    fun rendersCommonBoldMarkdownWithoutVisibleAsterisks() {
        assertEquals("这是重点内容", markdownDisplayText("这是**重点**内容").text)
        assertEquals("未完成的重点", markdownDisplayText("未完成的**重点").text)
    }

    @Test
    fun wrapsPlainStringChatResponsesAsAssistantContent() {
        assertEquals(
            "普通回复",
            parseResponseBody("/chat/completions", "\"普通回复\"")
                .getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        )
    }

    @Test
    fun rejectsHtmlPagesReturnedByChatCompletions() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            parseResponseBody("/chat/completions", "<!DOCTYPE html><html><body>Gateway</body></html>")
        }
        assertEquals("接口返回了网页内容，请检查中转站 API 地址，通常应以 /v1 结尾", error.message)
    }

    @Test
    fun rejectsPlainStringResponsesOutsideChatCompletions() {
        assertThrows(IllegalArgumentException::class.java) {
            parseResponseBody("/images/generations", "\"普通回复\"")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseResponseBody("/models", "plain text")
        }
    }

    @Test
    fun batchesStreamingDatabaseWritesByTimeOrContentSize() {
        assertEquals(false, shouldPersistChatProgress(100, 110, 1_000, 1_080))
        assertTrue(shouldPersistChatProgress(100, 400, 1_000, 1_080))
        assertTrue(shouldPersistChatProgress(100, 110, 1_000, 1_200))
    }

    @Test
    fun secretStorageFailureNeverFallsBackToPlaintext() {
        val error = assertThrows(IllegalStateException::class.java) {
            throwSecretStorageFailure(IllegalStateException("keystore unavailable"))
        }
        assertTrue(error.message.orEmpty().contains("安全保存"))
    }
}
