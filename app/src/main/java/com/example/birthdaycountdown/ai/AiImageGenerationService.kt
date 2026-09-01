package com.example.birthdaycountdown.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.birthdaycountdown.R
import com.example.birthdaycountdown.data.AiHistoryRepository
import com.example.birthdaycountdown.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class AiImageGenerationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val messageId = intent?.getLongExtra(EXTRA_MESSAGE_ID, 0L) ?: 0L
        val conversationId = intent?.getLongExtra(EXTRA_CONVERSATION_ID, 0L) ?: 0L
        val prompt = intent?.getStringExtra(EXTRA_PROMPT).orEmpty()
        val size = intent?.getStringExtra(EXTRA_SIZE)
        val quality = intent?.getStringExtra(EXTRA_QUALITY).orEmpty()
        if (messageId <= 0 || conversationId <= 0 || prompt.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification("AI 生图进行中", "任务会在后台继续执行"))
        serviceScope.launch {
            val db = AppDatabase.create(applicationContext)
            val repository = AiHistoryRepository(db.aiHistoryDao(), applicationContext)
            try {
                val message = db.aiHistoryDao().getMessage(messageId)
                if (message == null) return@launch
                if (message.status != "PENDING") {
                    if (isRecoverableAiStatus(message.status)) repository.failImage(messageId, "任务被系统中断，请一键重新生成")
                    return@launch
                }
                val prefs = AiPreferences(getSharedPreferences("settings", MODE_PRIVATE))
                val image = OpenAiCompatibleClient().generateImage(
                    prefs.read().image,
                    prompt,
                    size,
                    quality,
                    message.referenceImagePath?.let(repository::imageFile)
                ) { status -> repository.updateMessageStatus(messageId, status) }
                val bitmap = loadGeneratedBitmap(image)
                repository.updateMessageStatus(messageId, "SAVING")
                val actualSize = "${bitmap.width}x${bitmap.height}"
                val warning = compareImageSize(size, ImageOutputInfo(bitmap.width, bitmap.height))?.let {
                    "上游返回尺寸为 $actualSize，与请求的 $it 不一致，已按实际结果保存"
                }
                repository.completeImage(messageId, saveBitmapFile(repository, bitmap), actualSize, warning)
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Throwable) {
                repository.failImage(messageId, error.message?.take(200)?.ifBlank { "生成失败，请检查中转站配置后重试" } ?: "生成失败，请检查中转站配置后重试")
            } finally {
                stopSelf(startId)
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(title: String, content: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(content)
        .setOngoing(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "AI 生图任务", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "ai_image_generation"
        private const val NOTIFICATION_ID = 5102
        private const val EXTRA_MESSAGE_ID = "message_id"
        private const val EXTRA_CONVERSATION_ID = "conversation_id"
        private const val EXTRA_PROMPT = "prompt"
        private const val EXTRA_SIZE = "size"
        private const val EXTRA_QUALITY = "quality"

        fun intent(context: Context, messageId: Long, conversationId: Long, prompt: String, size: String?, quality: String) =
            Intent(context, AiImageGenerationService::class.java)
                .putExtra(EXTRA_MESSAGE_ID, messageId)
                .putExtra(EXTRA_CONVERSATION_ID, conversationId)
                .putExtra(EXTRA_PROMPT, prompt)
                .putExtra(EXTRA_SIZE, size)
                .putExtra(EXTRA_QUALITY, quality)

        private fun loadGeneratedBitmap(image: GeneratedImage): Bitmap {
            return when {
                !image.base64.isNullOrBlank() -> {
                    val bytes = android.util.Base64.decode(image.base64, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                !image.url.isNullOrBlank() -> {
                    val connection = (URL(image.url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20_000
                        readTimeout = 120_000
                    }
                    try {
                        if (connection.responseCode !in 200..299) error("图片地址返回 HTTP ${connection.responseCode}")
                        connection.inputStream.use(BitmapFactory::decodeStream)
                    } finally {
                        connection.disconnect()
                    }
                }
                else -> null
            } ?: error("无法读取生成图片")
        }

        private fun saveBitmapFile(repository: AiHistoryRepository, bitmap: Bitmap): String {
            val name = "image-${System.currentTimeMillis()}.png"
            repository.imageFile(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            return name
        }
    }
}
