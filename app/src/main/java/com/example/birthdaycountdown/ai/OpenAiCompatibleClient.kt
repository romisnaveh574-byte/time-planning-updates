package com.example.birthdaycountdown.ai

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.File

class OpenAiCompatibleClient {
    private class AiHttpException(val status: Int, val responseBody: String) :
        IllegalStateException(classifyAiHttpError(status, responseBody))

    fun listModels(config: AiEndpointConfig): List<String> {
        val response = request(config, "/models", "GET", null)
        val data = response.optJSONArray("data") ?: return emptyList()
        return buildList { for (i in 0 until data.length()) data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }?.let(::add) }
    }

    suspend fun chat(
        config: AiEndpointConfig,
        history: List<ChatTurn>,
        prompt: String,
        imageDataUrl: String?,
        onDelta: (suspend (String) -> Unit)? = null
    ): String = if (onDelta == null) {
        chatSync(config, history, prompt, imageDataUrl)
    } else {
        var hasReceivedReply = false
        try {
            chatStream(config, history, prompt, imageDataUrl) { reply ->
                if (reply.isNotBlank()) hasReceivedReply = true
                onDelta(reply)
            }
        } catch (error: AiHttpException) {
            if (error.status in setOf(400, 404) && error.responseBody.contains("stream", ignoreCase = true)) {
                chatSync(config, history, prompt, imageDataUrl)
            } else throw error
        } catch (error: Throwable) {
            if (shouldFallbackToSyncChat(error, hasReceivedReply)) chatSync(config, history, prompt, imageDataUrl) else throw error
        }
    }

    private fun chatSync(config: AiEndpointConfig, history: List<ChatTurn>, prompt: String, imageDataUrl: String?): String {
        val body = chatRequestBody(config, history, prompt, imageDataUrl, false)
        val response = request(config, "/chat/completions", "POST", body)
        return extractChatContent(response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.opt("content"))
            .takeIf { it.isNotBlank() }
            ?: error("中转站未返回对话内容")
    }

    private suspend fun chatStream(
        config: AiEndpointConfig,
        history: List<ChatTurn>,
        prompt: String,
        imageDataUrl: String?,
        onDelta: suspend (String) -> Unit
    ): String {
        require(config.baseUrl.isNotBlank()) { "请先填写接口地址" }
        require(config.apiKey.isNotBlank()) { "请先填写 API Key" }
        require(config.model.isNotBlank()) { "请先选择模型" }
        val connection = (URL(normalizeBaseUrl(config.baseUrl) + "/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 300_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(chatRequestBody(config, history, prompt, imageDataUrl, true).toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            if (status !in 200..299) {
                val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                throw AiHttpException(status, responseBody)
            }
            if (!connection.contentType.orEmpty().contains("text/event-stream", ignoreCase = true)) {
                val response = JSONObject(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val reply = extractChatContent(response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.opt("content"))
                    .takeIf { it.isNotBlank() }
                    ?: error("中转站未返回对话内容")
                onDelta(reply)
                return reply
            }
            val reply = StringBuilder()
            stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    if (!line.startsWith("data:")) return@forEach
                    val payload = line.removePrefix("data:").trim()
                    if (payload == "[DONE]") return@forEach
                    val delta = runCatching {
                        extractChatContent(JSONObject(payload).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.opt("content"))
                    }.getOrDefault("")
                    if (delta.isNotEmpty()) {
                        reply.append(delta)
                        onDelta(reply.toString())
                    }
                }
            }
            return reply.toString().takeIf { it.isNotBlank() } ?: error("中转站未返回流式对话内容")
        } finally {
            connection.disconnect()
        }
    }

    private fun chatRequestBody(config: AiEndpointConfig, history: List<ChatTurn>, prompt: String, imageDataUrl: String?, stream: Boolean): JSONObject {
        val content: Any = if (imageDataUrl.isNullOrBlank()) prompt else JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", prompt))
            put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", imageDataUrl)))
        }
        val requestMessages = JSONArray()
        history.forEach { turn ->
            val historyContent: Any = turn.imageDataUrl?.let { image ->
                JSONArray().put(JSONObject().put("type", "text").put("text", turn.text))
                    .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", image)))
            } ?: turn.text
            requestMessages.put(JSONObject().put("role", turn.role).put("content", historyContent))
        }
        requestMessages.put(JSONObject().put("role", "user").put("content", content))
        return JSONObject().put("model", config.model).put("messages", requestMessages).apply { if (stream) put("stream", true) }
    }

    private fun extractChatContent(contentValue: Any?): String = when (contentValue) {
            is String -> contentValue
            is JSONArray -> buildString { for (i in 0 until contentValue.length()) append(contentValue.optJSONObject(i)?.optString("text").orEmpty()) }
            else -> ""
        }

    suspend fun chat(config: AiEndpointConfig, prompt: String, imageDataUrl: String?): String = chat(config, emptyList(), prompt, imageDataUrl)

    suspend fun generateImage(
        config: AiEndpointConfig,
        prompt: String,
        size: String?,
        quality: String,
        referenceImage: File? = null,
        onProgress: suspend (String) -> Unit = {}
    ): GeneratedImage {
        val response = if (referenceImage == null) {
            val body = JSONObject().put("model", config.model).put("prompt", prompt).put("quality", quality)
            size?.let { body.put("size", it) }
            requestAsyncOrSync(config, body, onProgress)
        } else requestImageEdit(config, prompt, size, quality, referenceImage, onProgress)
        val item = response.optJSONArray("data")?.optJSONObject(0)
            ?: JSONObject().apply {
                put("url", response.optString("url"))
                put("b64_json", response.optString("b64_json"))
            }
        if (item.optString("task_id").isNotBlank() || response.optString("task_id").isNotBlank()) error("中转站返回了异步生图任务，请使用支持同步结果的模型")
        return GeneratedImage(item.optString("url").takeIf { it.isNotBlank() }, item.optString("b64_json").takeIf { it.isNotBlank() })
    }

    private suspend fun requestAsyncOrSync(config: AiEndpointConfig, body: JSONObject, onProgress: suspend (String) -> Unit): JSONObject {
        onProgress("SUBMITTING")
        val async: JSONObject? = try {
            request(config, "/images/generations/async", "POST", body, readTimeoutMs = 60_000)
        } catch (error: AiHttpException) {
            if (isAsyncImageUnsupported(error.status, error.responseBody)) null else throw error
        }
        val taskId = async?.optString("task_id")?.takeIf { it.isNotBlank() }
        if (taskId == null) {
            if (async != null && hasCompletedImagePayload(async)) return async
            onProgress("PROCESSING")
            return request(config, "/images/generations", "POST", body, readTimeoutMs = 300_000)
        }
        return pollAsyncTask(config, taskId, onProgress)
    }

    private suspend fun pollAsyncTask(config: AiEndpointConfig, taskId: String, onProgress: suspend (String) -> Unit): JSONObject {
        val deadline = System.currentTimeMillis() + 30 * 60_000L
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(3000)
            val task = request(config, "/images/tasks/$taskId", "GET", null, readTimeoutMs = 30_000)
            when (task.optString("status").lowercase()) {
                "completed", "succeeded", "success" -> return task.optJSONObject("result") ?: task
                "failed", "expired", "cancelled", "canceled" -> error(task.optJSONObject("error")?.optString("message") ?: "异步生图任务失败")
                "queued", "pending" -> onProgress("QUEUED")
                "processing" -> onProgress("PROCESSING")
                else -> error("异步生图任务返回未知状态")
            }
        }
        error("异步生图任务超过30分钟，已停止等待")
    }

    private suspend fun requestImageEdit(config: AiEndpointConfig, prompt: String, size: String?, quality: String, image: File, onProgress: suspend (String) -> Unit): JSONObject {
        onProgress("SUBMITTING")
        val async = try {
            requestMultipartImageEdit(config, "/images/edits/async", prompt, size, quality, image, 60_000)
        } catch (error: AiHttpException) {
            if (isAsyncImageUnsupported(error.status, error.responseBody)) null else throw error
        }
        val taskId = async?.optString("task_id")?.takeIf { it.isNotBlank() }
        return if (taskId != null) pollAsyncTask(config, taskId, onProgress)
        else if (async != null && hasCompletedImagePayload(async)) async
        else {
            onProgress("PROCESSING")
            requestMultipartImageEdit(config, "/images/edits", prompt, size, quality, image, 300_000)
        }
    }

    private fun requestMultipartImageEdit(
        config: AiEndpointConfig,
        path: String,
        prompt: String,
        size: String?,
        quality: String,
        image: File,
        readTimeoutMs: Int
    ): JSONObject {
        require(image.isFile) { "参考图文件不存在" }
        require(config.baseUrl.isNotBlank()) { "请先填写接口地址" }
        require(config.apiKey.isNotBlank()) { "请先填写 API Key" }
        require(config.model.isNotBlank()) { "请先选择模型" }
        val boundary = "----TimePlanning${System.currentTimeMillis()}"
        val connection = (URL(normalizeBaseUrl(config.baseUrl) + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = readTimeoutMs
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
                val mimeType = referenceImageMimeType(image.name)
                val extension = mimeType.substringAfterLast('/')
                output.write("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"reference.$extension\"\r\nContent-Type: $mimeType\r\n\r\n".toByteArray(Charsets.UTF_8))
                image.inputStream().use { it.copyTo(output) }
                output.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                throw AiHttpException(status, message.ifBlank { text })
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
                throw AiHttpException(status, message.ifBlank { text })
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
