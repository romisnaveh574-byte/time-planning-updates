package com.example.birthdaycountdown.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.io.File

enum class AiMode { CHAT, IMAGE }

@Entity(tableName = "ai_conversations")
data class AiConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

@Entity(
    tableName = "ai_messages",
    foreignKeys = [ForeignKey(entity = AiConversationEntity::class, parentColumns = ["id"], childColumns = ["conversationId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("conversationId")]
)
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val text: String,
    val imagePath: String? = null,
    val referenceImagePath: String? = null,
    val size: String? = null,
    val quality: String? = null,
    val actualSize: String? = null,
    val warning: String? = null,
    val errorMessage: String? = null,
    val status: String = "DONE",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface AiHistoryDao {
    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC") fun observeConversations(): Flow<List<AiConversationEntity>>
    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC") fun observeMessages(conversationId: Long): Flow<List<AiMessageEntity>>
    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC, id ASC") suspend fun getMessages(conversationId: Long): List<AiMessageEntity>
    @Query("SELECT * FROM ai_messages WHERE id = :id") suspend fun getMessage(id: Long): AiMessageEntity?
    @Insert suspend fun insertConversation(value: AiConversationEntity): Long
    @Insert suspend fun insertMessage(value: AiMessageEntity): Long
    @Query("UPDATE ai_conversations SET updatedAt = :updatedAt WHERE id = :id") suspend fun touchConversation(id: Long, updatedAt: Long)
    @Query("UPDATE ai_messages SET status = :status, imagePath = :imagePath, actualSize = :actualSize, warning = :warning, errorMessage = NULL WHERE id = :id") suspend fun updateMessage(id: Long, status: String, imagePath: String?, actualSize: String?, warning: String?)
    @Query("UPDATE ai_messages SET status = :status WHERE id = :id") suspend fun updateMessageStatus(id: Long, status: String)
    @Query("UPDATE ai_messages SET text = :text, status = :status WHERE id = :id") suspend fun updateMessageText(id: Long, text: String, status: String)
    @Query("UPDATE ai_messages SET status = 'FAILED', errorMessage = :errorMessage WHERE id = :id") suspend fun failMessage(id: Long, errorMessage: String)
    @Query("DELETE FROM ai_conversations WHERE id = :id") suspend fun deleteConversation(id: Long)
    @Query("UPDATE ai_messages SET status = 'FAILED', errorMessage = :message WHERE status IN ('PENDING','SUBMITTING','QUEUED','PROCESSING','SAVING') AND createdAt < :cutoff") suspend fun failStaleMessages(cutoff: Long, message: String): Int
}

class AiHistoryRepository(private val dao: AiHistoryDao, private val context: Context) {
    val conversations: Flow<List<AiConversationEntity>> = dao.observeConversations()
    fun messages(id: Long): Flow<List<AiMessageEntity>> = dao.observeMessages(id)
    suspend fun newConversation(mode: AiMode, title: String = if (mode == AiMode.CHAT) "新对话" else "新生图记录"): Long = dao.insertConversation(AiConversationEntity(mode = mode.name, title = title))
    suspend fun append(message: AiMessageEntity): Long {
        val id = dao.insertMessage(message)
        dao.touchConversation(message.conversationId, System.currentTimeMillis())
        return id
    }
    suspend fun addPendingImage(conversationId: Long, prompt: String, size: String?, quality: String, referenceImagePath: String?): Long {
        val id = dao.insertMessage(AiMessageEntity(conversationId = conversationId, role = "assistant", text = prompt, size = size, quality = quality, referenceImagePath = referenceImagePath, status = "PENDING"))
        dao.touchConversation(conversationId, System.currentTimeMillis())
        return id
    }
    suspend fun completeImage(messageId: Long, path: String, actualSize: String? = null, warning: String? = null) = dao.updateMessage(messageId, "DONE", path, actualSize, warning)
    suspend fun failImage(messageId: Long, errorMessage: String) = dao.failMessage(messageId, errorMessage)
    suspend fun failMessage(messageId: Long, errorMessage: String) = dao.failMessage(messageId, errorMessage)
    suspend fun updateMessageStatus(messageId: Long, status: String) = dao.updateMessageStatus(messageId, status)
    suspend fun updateMessageText(messageId: Long, text: String, status: String) = dao.updateMessageText(messageId, text, status)
    suspend fun retryImage(messageId: Long) = dao.updateMessage(messageId, "PENDING", null, null, null)
    suspend fun failStaleMessages(maxAgeMs: Long = 30 * 60 * 1000L) = dao.failStaleMessages(System.currentTimeMillis() - maxAgeMs, "任务等待时间过长，请重新发送或一键重新生成")
    suspend fun deleteConversation(id: Long) {
        val paths = dao.getMessages(id).flatMap { listOfNotNull(it.imagePath, it.referenceImagePath) }.distinct()
        dao.deleteConversation(id)
        paths.forEach { File(context.filesDir, it).delete() }
    }
    fun imageFile(name: String) = File(context.filesDir, "ai_images").apply { mkdirs() }.resolve(name)
}
