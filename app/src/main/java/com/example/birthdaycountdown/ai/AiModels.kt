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

data class ChatTurn(val role: String, val text: String)

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

fun filterAiModels(models: List<String>, image: Boolean): List<String> = models.filter { id ->
    val name = id.lowercase()
    if (image) name.contains("image") || name.contains("dall") || name.contains("flux") || name.contains("sd")
    else !(name.contains("image") || name.contains("dall") || name.contains("flux") || name.contains("stable-diffusion"))
}.ifEmpty { models }

fun classifyAiHttpError(status: Int, body: String): String = when (status) {
    401, 403 -> "API Key 无效或无权限，请检查配置"
    404 -> "接口地址或路径不存在，请检查中转站地址"
    408, 429 -> "请求超时或频率受限，请稍后重试"
    in 500..599 -> body.ifBlank { "中转站服务异常，请稍后重试" }
    else -> body.ifBlank { "请求失败（HTTP $status）" }
}
