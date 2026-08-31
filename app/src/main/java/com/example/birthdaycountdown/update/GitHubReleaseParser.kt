package com.example.birthdaycountdown.update

import org.json.JSONObject

data class ReleaseInfo(val version: AppVersion, val downloadUrl: String, val sha256: String, val notes: String)

object GitHubReleaseParser {
    fun parse(json: String): ReleaseInfo? = runCatching {
        val root = JSONObject(json)
        if (root.optBoolean("draft") || root.optBoolean("prerelease")) return null
        val tag = root.optString("tag_name")
        val parts = parseVersionName(tag) ?: return null
        val assets = root.optJSONArray("assets") ?: return null
        var url: String? = null; var digest: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name") == "time-planning.apk") { url = a.optString("browser_download_url"); digest = a.optString("digest") }
        }
        val hash = digest?.removePrefix("sha256:")?.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) } ?: return null
        if (url?.startsWith("https://") != true) return null
        ReleaseInfo(AppVersion(parts[0] * 1_000_000L + parts[1] * 1_000L + parts[2], tag), url, hash.lowercase(), root.optString("body"))
    }.getOrNull()
}

