package com.example.birthdaycountdown.update

import java.io.InputStream
import java.io.OutputStream
import kotlin.math.roundToInt

data class DownloadProgress(
    val receivedBytes: Long,
    val totalBytes: Long,
    val isComplete: Boolean
) {
    val percentage: Int? = if (totalBytes > 0) ((receivedBytes * 100.0 / totalBytes).coerceIn(0.0, 100.0)).roundToInt() else null

    companion object {
        fun copy(input: InputStream, output: OutputStream, totalBytes: Long, onProgress: (DownloadProgress) -> Unit) {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var received = 0L
            onProgress(DownloadProgress(0, totalBytes, false))
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                received += count
                onProgress(DownloadProgress(received, totalBytes, false))
            }
            output.flush()
            onProgress(DownloadProgress(received, totalBytes, true))
        }
    }
}
