package com.example.birthdaycountdown.ai

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.File

class OpenAiCompatibleClient {
    fun listModels(config: AiEndpointConfig): List<String> {
        val response = request(config, "/models", "GET", null)
        val data = response.optJSONArray("data") ?: return emptyList()
        return buildList { for (i in 0 until data.length()) data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }?.let(::add) }
    }

    fun chat(config: AiEndpointConfig, history: List<ChatTurn>, prompt: String, imageDataUrl: String?): String {
        val content: Any = if (imageDataUrl.isNullOrBlank()) prompt else JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", prompt))
            put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageDataUrl)))
        }
        val requestMessages = JSONArray()
        history.forEach { requestMessages.put(JSONObject().put("role", it.role).put("content", it.text)) }
        requestMessages.put(JSONObject().put("role", "user").put("content", content))
        val body = JSONObject().put("model", config.model).put("messages", requestMessages)
        val response = request(config, "/chat/completions", "POST", body)
        val contentValue = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.opt("content")
        val text = when (contentValue) {
            is String -> contentValue
            is JSONArray -> buildString { for (i in 0 until contentValue.length()) append(contentValue.optJSONObject(i)?.optString("text").orEmpty()) }
            else -> ""
        }
        return text.takeIf { it.isNotBlank() }
            ?: error("中转站未返回对话内容")
    }

    fun chat(config: AiEndpointConfig, prompt: String, imageDataUrl: String?): String = chat(config, emptyList(), prompt, imageDataUrl)

    fun generateImage(config: AiEndpointConfig, prompt: String, size: String?, quality: String, referenceImage: File? = null): GeneratedImage {
        val response = if (referenceImage == null) {
            val body = JSONObject().put("model", config.model).put("prompt", prompt).put("quality", quality)
            size?.let { body.put("size", it) }
            request(config, "/images/generations", "POST", body, readTimeoutMs = 300_000)
        } else requestImageEdit(config, prompt, size, quality, referenceImage)
        val item = response.optJSONArray("data")?.optJSONObject(0)
            ?: JSONObject().apply {
                put("url", response.optString("url"))
                put("b64_json", response.optString("b64_json"))
            }
        if (item.optString("task_id").isNotBlank() || response.optString("task_id").isNotBlank()) error("中转站返回了异步生图任务，请使用支持同步结果的模型")
        return GeneratedImage(item.optString("url").takeIf { it.isNotBlank() }, item.optString("b64_json").takeIf { it.isNotBlank() })
    }

    private fun requestImageEdit(config: AiEndpointConfig, prompt: String, size: String?, quality: String, image: File): JSONObject {
        require(image.isFile) { "参考图文件不存在" }
        require(config.baseUrl.isNotBlank()) { "请先填写接口地址" }
        require(config.apiKey.isNotBlank()) { "请先填写 API Key" }
        require(config.model.isNotBlank()) { "请先选择模型" }
        val boundary = "----TimePlanning${System.currentTimeMillis()}"
        val connection = (URL(normalizeBaseUrl(config.baseUrl) + "/images/edits").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 300_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        try {
            connection.outputStream.buffered().use { output ->
                fun field(name: String, value: String) {
                    output.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray(Charsets.UTF_8))
                }
                field("model", config.model)
                field("prompt", prompt)
                field("quality", quality)
                size?.let { field("size", it) }
                output.write("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"reference.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray(Charsets.UTF_8))
                image.inputStream().use { it.copyTo(output) }
                output.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                error(classifyAiHttpError(status, message.ifBlank { text }))
            }
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    fun fileToDataUrl(input: InputStream, mimeType: String): String {
        val bytes = input.use { it.readBytes() }
        return "data:$mimeType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    }

    private fun request(config: AiEndpointConfig, path: String, method: String, body: JSONObject?, readTimeoutMs: Int = 60_000): JSONObject {
        require(config.baseUrl.isNotBlank()) { "请先填写接口地址" }
        require(config.apiKey.isNotBlank()) { "请先填写 API Key" }
        require(config.model.isNotBlank() || path == "/models") { "请先选择模型" }
        val endpoint = normalizeBaseUrl(config.baseUrl) + path
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = readTimeoutMs
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                error(classifyAiHttpError(status, message.ifBlank { text }))
            }
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) { "接口地址必须以 http:// 或 https:// 开头" }
        return normalizeAiBaseUrl(trimmed)
    }
}
