package com.example.birthdaycountdown.ai

data class AiEndpointConfig(
    val baseUrl: String = "https://wawapii.com/v1",
    val apiKey: String = "",
    val model: String = ""
)

data class AiSettings(
    val chat: AiEndpointConfig = AiEndpointConfig(),
    val image: AiEndpointConfig = AiEndpointConfig()
)

data class GeneratedImage(val url: String?, val base64: String?)

data class ImageOutputInfo(val width: Int, val height: Int) {
    val size: String get() = "${width}x${height}"
}

data class ChatTurn(val role: String, val text: String, val imageDataUrl: String? = null)

fun normalizeAiBaseUrl(raw: String): String {
    val trimmed = raw.trim().trimEnd('/')
    require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) { "接口地址必须以 http:// 或 https:// 开头" }
    val uri = java.net.URI(trimmed)
    var path = uri.path.orEmpty().trimEnd('/')
    listOf("/model-plaza", "/models", "/v1").forEach { suffix -> if (path.endsWith(suffix)) path = path.removeSuffix(suffix).trimEnd('/') }
    return buildString {
        append(uri.scheme).append("://").append(uri.rawAuthority)
        if (path.isNotBlank()) append(path)
        append("/v1")
    }
}

fun buildChatMessages(history: List<ChatTurn>, prompt: String): List<ChatTurn> = history + ChatTurn("user", prompt)

fun validateAiImageSize(bytes: Long, maxBytes: Long = 10L * 1024 * 1024) {
    require(bytes in 1..maxBytes) { "图片大小不能超过 10 MB" }
}

fun imageSizeFor(resolution: String, aspectRatio: String, sourceWidth: Int? = null, sourceHeight: Int? = null): String? {
    val edge = when (resolution) { "2K" -> 2048; "4K" -> 4096; else -> 1024 }
    if (aspectRatio == "原比例") {
        val width = sourceWidth ?: return null
        val height = sourceHeight ?: return null
        val sourceEdge = maxOf(width, height)
        return if (width >= height) "$edge" + "x" + (height * edge / sourceEdge)
        else (width * edge / sourceEdge).toString() + "x" + edge
    }
    val parts = aspectRatio.split(":")
    val widthRatio = parts.getOrNull(0)?.toIntOrNull() ?: 1
    val heightRatio = parts.getOrNull(1)?.toIntOrNull() ?: 1
    return if (widthRatio >= heightRatio) "$edge" + "x" + (edge * heightRatio / widthRatio)
    else (edge * widthRatio / heightRatio).toString() + "x" + edge
}

fun compareImageSize(requested: String?, actual: ImageOutputInfo): String? =
    requested?.takeIf { it != actual.size }

fun referenceImageMimeType(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    else -> "image/jpeg"
}

fun referenceImageExtension(mimeType: String?): String = when (mimeType?.lowercase()) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    else -> "jpg"
}

fun isAsyncImageUnsupported(status: Int, responseBody: String): Boolean {
    val body = responseBody.lowercase()
    return status == 404 && (body.contains("not_found_error") || body.contains("async image tasks are not enabled"))
}

fun hasCompletedImagePayload(response: org.json.JSONObject): Boolean {
    val item = response.optJSONArray("data")?.optJSONObject(0)
    return !item?.optString("url").isNullOrBlank() || !item?.optString("b64_json").isNullOrBlank() ||
        response.optString("url").isNotBlank() || response.optString("b64_json").isNotBlank()
}

fun isActiveAiStatus(status: String): Boolean = status in setOf("PENDING", "SUBMITTING", "QUEUED", "PROCESSING", "SAVING")

fun isRecoverableAiStatus(status: String): Boolean = isActiveAiStatus(status)

fun needsAiSetup(config: AiEndpointConfig): Boolean = config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()

fun imageGenerationStatusLabel(status: String): String = when (status) {
    "SUBMITTING" -> "正在提交"
    "QUEUED" -> "排队中"
    "SAVING" -> "正在保存"
    else -> "正在生成"
}

fun aiHistoryModeLabel(mode: String): String = when (mode) {
    "CHAT" -> "AI 对话"
    else -> "AI 生图"
}

fun gallerySaveResultLabel(saved: Boolean): String = if (saved) "已保存到相册" else "保存到相册失败"

fun filterAiModels(models: List<String>, image: Boolean): List<String> = models.filter { id ->
    val name = id.lowercase()
    if (image) name.contains("image") || name.contains("dall") || name.contains("flux") || name.contains("sd")
    else !(name.contains("image") || name.contains("dall") || name.contains("flux") || name.contains("stable-diffusion"))
}.ifEmpty { models }

fun classifyAiHttpError(status: Int, body: String): String = when {
    status in 500..599 && isProviderAccountUnavailable(body) -> "上游账号暂时无可用容量，请稍后重试或更换生图模型"
    status in setOf(401, 403) -> "API Key 无效或无权限，请检查配置"
    status == 404 -> "接口地址或路径不存在，请检查中转站地址"
    status in setOf(408, 429) -> "请求超时或频率受限，请稍后重试"
    status in 500..599 -> body.ifBlank { "中转站服务异常，请稍后重试" }
    else -> body.ifBlank { "请求失败（HTTP $status）" }
}

fun shouldFallbackToSyncChat(error: Throwable, hasReceivedReply: Boolean): Boolean =
    !hasReceivedReply && error is java.net.SocketException && error.message.orEmpty().let {
        it.contains("connection abort", ignoreCase = true) || it.contains("connection reset", ignoreCase = true)
    }

fun aiFailureMessage(error: Throwable, fallback: String): String {
    val message = error.message.orEmpty()
    return when {
        isProviderAccountUnavailable(message) -> "上游账号暂时无可用容量，请稍后重试或更换生图模型"
        error is java.net.SocketException || message.contains("connection abort", ignoreCase = true) || message.contains("connection reset", ignoreCase = true) ->
            "网络连接被中断，已尝试兼容请求；请稍后重试"
        else -> message.take(200).ifBlank { fallback }
    }
}

private fun isProviderAccountUnavailable(message: String): Boolean =
    message.contains("no available compatible accounts", ignoreCase = true)
