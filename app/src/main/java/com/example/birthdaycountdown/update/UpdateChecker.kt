package com.example.birthdaycountdown.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateCheckResult {
    data class Available(val release: ReleaseInfo) : UpdateCheckResult
    object Latest : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

class UpdateChecker(private val owner: String, private val repo: String) {
    suspend fun check(current: AppVersion): ReleaseInfo? = when (val result = checkResult(current)) {
        is UpdateCheckResult.Available -> result.release
        else -> null
    }

    suspend fun checkResult(current: AppVersion): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://api.github.com/repos/$owner/$repo/releases/latest").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            try {
                if (conn.responseCode !in 200..299) return@runCatching UpdateCheckResult.Failed("更新服务返回 ${conn.responseCode}")
                val release = GitHubReleaseParser.parse(conn.inputStream.bufferedReader().use { it.readText() })
                if (release != null && release.version > current) UpdateCheckResult.Available(release) else UpdateCheckResult.Latest
            } finally {
                conn.disconnect()
            }
        }.getOrElse { UpdateCheckResult.Failed(it.message ?: "无法连接更新服务") }
    }
}
