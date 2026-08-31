package com.example.birthdaycountdown.update

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateDownloader {
    sealed interface Result {
        data class Ready(val file: File) : Result
        data class Failed(val message: String) : Result
    }

    fun clearDownloadedPackages(context: Context) {
        updateDirectory(context).listFiles()?.forEach { file ->
            if (file.isFile && file.extension.equals("apk", ignoreCase = true)) file.delete()
        }
    }

    fun download(context: Context, release: ReleaseInfo, onProgress: (DownloadProgress) -> Unit = {}): Result = runCatching {
        val dir = updateDirectory(context)
        val out = File(dir, UpdatePackageFileName.forVersion(release.version.name))
        if (out.exists()) out.delete()
        val c = URL(release.downloadUrl).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        try {
            if (c.responseCode !in 200..299) return Result.Failed("服务器返回 ${c.responseCode}")
            c.inputStream.use { input -> out.outputStream().use { output -> DownloadProgress.copy(input, output, c.contentLengthLong, onProgress) } }
            if (!ApkVerifier.verify(context, out, release)) {
                out.delete()
                Result.Failed("安装包校验失败")
            } else {
                Result.Ready(out)
            }
        } finally {
            c.disconnect()
        }
    }.getOrElse { Result.Failed(it.message ?: "下载失败") }

    private fun updateDirectory(context: Context): File =
        File(context.getExternalFilesDir("Download"), "updates").apply { mkdirs() }
}
