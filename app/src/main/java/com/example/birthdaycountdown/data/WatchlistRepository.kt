package com.example.birthdaycountdown.data

import androidx.room.withTransaction
import com.example.birthdaycountdown.domain.resequenceWatchRecords
import kotlinx.coroutines.flow.Flow

class WatchlistRepository(
    private val database: AppDatabase,
    private val dao: WatchlistDao
) {
    val categories: Flow<List<WatchCategoryEntity>> = dao.observeCategories()
    val records: Flow<List<WatchRecordEntity>> = dao.observeRecords()

    suspend fun allCategories(): List<WatchCategoryEntity> = dao.getCategories()
    suspend fun allRecords(): List<WatchRecordEntity> = dao.getRecords()

    suspend fun ensureDefaultCategories() {
        if (dao.categoryCount() == 0) {
            DEFAULT_CATEGORIES.forEachIndexed { index, name ->
                dao.insertCategory(WatchCategoryEntity(name = name, sortOrder = index))
            }
        }
    }

    suspend fun saveRecord(record: WatchRecordEntity): Long {
        val normalized = record.copy(title = record.title.trim())
        return if (normalized.id == 0L) {
            dao.insertRecord(normalized.copy(sortOrder = dao.nextRecordSortOrder()))
        } else {
            dao.updateRecord(normalized)
            normalized.id
        }
    }

    suspend fun setEpisode(record: WatchRecordEntity, currentEpisode: Int) {
        dao.updateRecord(record.copy(currentEpisode = currentEpisode.coerceIn(0, record.totalEpisodes ?: Int.MAX_VALUE), lastWatchedAt = System.currentTimeMillis()))
    }

    suspend fun adjustEpisode(recordId: Long, delta: Int) = dao.adjustEpisode(recordId, delta.coerceIn(-1, 1), System.currentTimeMillis())

    suspend fun deleteRecord(record: WatchRecordEntity) = dao.deleteRecord(record)

    suspend fun reorderRecords(records: List<WatchRecordEntity>) {
        dao.updateRecords(resequenceWatchRecords(records))
    }

    suspend fun saveCategory(category: WatchCategoryEntity): Long {
        val normalized = category.copy(name = category.name.trim())
        require(dao.categoryNameCount(normalized.name, normalized.id) == 0) { "分类名称已存在" }
        return if (normalized.id == 0L) {
            dao.insertCategory(normalized.copy(sortOrder = dao.nextCategorySortOrder()))
        } else {
            dao.updateCategory(normalized)
            normalized.id
        }
    }

    suspend fun reorderCategories(categories: List<WatchCategoryEntity>) {
        dao.updateCategories(categories.mapIndexed { index, category -> category.copy(sortOrder = index) })
    }

    suspend fun deleteCategoryAndMoveRecords(sourceCategoryId: Long, targetCategoryId: Long?) {
        database.withTransaction {
            require(dao.categoryCount() > 1) { "至少保留一个分类" }
            val recordCount = dao.recordCountForCategory(sourceCategoryId)
            if (recordCount > 0) {
                require(targetCategoryId != null && targetCategoryId != sourceCategoryId) { "请选择接收记录的分类" }
                require(dao.categoryExists(targetCategoryId) > 0) { "接收分类不存在" }
                dao.moveRecords(sourceCategoryId, targetCategoryId)
            }
            dao.deleteCategoryById(sourceCategoryId)
        }
    }

    suspend fun import(categories: List<WatchCategoryEntity>, records: List<WatchRecordEntity>) {
        database.withTransaction {
            val categoryIds = categories.associate { category ->
                val name = category.name.trim()
                val id = dao.findCategoryIdByName(name)
                    ?: dao.insertCategory(category.copy(id = 0L, name = name, sortOrder = dao.nextCategorySortOrder()))
                category.id to id
            }
            val existingRecords = dao.getRecords().toMutableList()
            records.forEach { record ->
                categoryIds[record.categoryId]?.let { mappedCategoryId ->
                    val normalizedTitle = record.title.trim()
                    if (existingRecords.none { it.categoryId == mappedCategoryId && it.title.trim().equals(normalizedTitle, ignoreCase = true) }) {
                        val inserted = record.copy(id = 0L, categoryId = mappedCategoryId, title = normalizedTitle, sortOrder = dao.nextRecordSortOrder())
                        val insertedId = dao.insertRecord(inserted)
                        existingRecords += inserted.copy(id = insertedId)
                    }
                }
            }
        }
    }

    companion object {
        val DEFAULT_CATEGORIES = listOf("电视剧", "电影", "动漫", "短剧")
    }
}
