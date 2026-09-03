package com.example.birthdaycountdown.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Base64
import androidx.core.app.NotificationCompat
import com.example.birthdaycountdown.R
import com.example.birthdaycountdown.data.AiHistoryRepository
import com.example.birthdaycountdown.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AiChatService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val responseMessageId = intent?.getLongExtra(EXTRA_RESPONSE_MESSAGE_ID, 0L) ?: 0L
        val userMessageId = intent?.getLongExtra(EXTRA_USER_MESSAGE_ID, 0L) ?: 0L
        val conversationId = intent?.getLongExtra(EXTRA_CONVERSATION_ID, 0L) ?: 0L
        val prompt = intent?.getStringExtra(EXTRA_PROMPT).orEmpty()
        if (responseMessageId <= 0 || userMessageId <= 0 || conversationId <= 0) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification("AI 对话进行中", "任务会在后台继续执行"))
        serviceScope.launch {
            val db = AppDatabase.create(applicationContext)
            val repository = AiHistoryRepository(db.aiHistoryDao(), applicationContext)
            try {
                val current = db.aiHistoryDao().getMessage(responseMessageId)
                if (current == null) return@launch
                if (current.status != "PENDING") {
                    if (isRecoverableAiStatus(current.status)) repository.failMessage(responseMessageId, "任务被系统中断，请重新发送上一条消息")
                    return@launch
                }
                val history = db.aiHistoryDao().getMessages(conversationId)
                    .filter { it.id != responseMessageId && it.id != userMessageId && it.status == "DONE" }
                    .map { ChatTurn(it.role, it.text, it.imagePath?.let { path -> dataUrlForStoredImage(repository, path) }) }
                repository.updateMessageStatus(responseMessageId, "SUBMITTING")
                var persistedLength = 0
                var persistedAt = System.currentTimeMillis()
                val reply = OpenAiCompatibleClient().chat(
                    AiPreferences(getSharedPreferences("settings", MODE_PRIVATE)).read().chat,
                    history,
                    prompt,
                    db.aiHistoryDao().getMessage(userMessageId)?.imagePath?.let { dataUrlForStoredImage(repository, it) }
                ) { partial ->
                    val now = System.currentTimeMillis()
                    if (shouldPersistChatProgress(persistedLength, partial.length, persistedAt, now)) {
                        repository.updateMessageText(responseMessageId, partial, "PROCESSING")
                        persistedLength = partial.length
                        persistedAt = now
                    }
                }
                repository.updateMessageText(responseMessageId, reply, "DONE")
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Throwable) {
                repository.failMessage(responseMessageId, aiFailureMessage(error, "回复失败，请检查中转站配置后重试"))
            } finally {
                stopSelf(startId)
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "AI 对话任务", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(title: String, content: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(content)
        .setOngoing(true)
        .build()

    private fun dataUrlForStoredImage(repository: AiHistoryRepository, path: String): String {
        val source = BitmapFactory.decodeFile(repository.imageFile(path).absolutePath) ?: error("图片文件无法读取")
        val maxEdge = maxOf(source.width, source.height)
        val scale = if (maxEdge > 2048) 2048f / maxEdge else 1f
        val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true) else source
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        if (bitmap !== source) bitmap.recycle()
        validateAiImageSize(output.size().toLong())
        return "data:image/jpeg;base64,${Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)}"
    }

    companion object {
        private const val CHANNEL_ID = "ai_chat"
        private const val NOTIFICATION_ID = 5101
        private const val EXTRA_RESPONSE_MESSAGE_ID = "response_message_id"
        private const val EXTRA_USER_MESSAGE_ID = "user_message_id"
        private const val EXTRA_CONVERSATION_ID = "conversation_id"
        private const val EXTRA_PROMPT = "prompt"

        fun intent(context: Context, responseMessageId: Long, userMessageId: Long, conversationId: Long, prompt: String) =
            Intent(context, AiChatService::class.java)
                .putExtra(EXTRA_RESPONSE_MESSAGE_ID, responseMessageId)
                .putExtra(EXTRA_USER_MESSAGE_ID, userMessageId)
                .putExtra(EXTRA_CONVERSATION_ID, conversationId)
                .putExtra(EXTRA_PROMPT, prompt)
    }
}

internal fun shouldPersistChatProgress(
    persistedLength: Int,
    currentLength: Int,
    persistedAt: Long,
    now: Long
): Boolean = currentLength - persistedLength >= 256 || now - persistedAt >= 150
