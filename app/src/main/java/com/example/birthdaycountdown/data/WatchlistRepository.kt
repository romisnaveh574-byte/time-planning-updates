package com.example.birthdaycountdown.data

import androidx.room.withTransaction
import com.example.birthdaycountdown.domain.resequenceWatchRecords
import kotlinx.coroutines.flow.Flow

class WatchlistRepository(
    private val database: AppDatabase?,
    private val dao: WatchlistDao,
    private val transactionRunner: suspend (suspend () -> Unit) -> Unit = { block ->
        requireNotNull(database).withTransaction { block() }
    }
) {
    val categories: Flow<List<WatchCategoryEntity>> = dao.observeCategories()
    val records: Flow<List<WatchRecordEntity>> = dao.observeRecords()
    val watchStatuses: Flow<List<WatchStatusEntity>> = dao.observeWatchStatuses()

    suspend fun allCategories(): List<WatchCategoryEntity> = dao.getCategories()
    suspend fun allRecords(): List<WatchRecordEntity> = dao.getRecords()

    suspend fun allWatchStatuses(): List<WatchStatusEntity> = dao.getWatchStatuses()

    suspend fun ensureBuiltInStatuses() {
        val existingIds = dao.getWatchStatuses().mapTo(mutableSetOf()) { it.id }
        WatchStatusEntity.builtIns
            .filterNot { it.id in existingIds }
            .forEach { dao.insertWatchStatus(it) }
    }

    suspend fun addWatchStatus(name: String): WatchStatusEntity {
        val normalizedName = normalizeWatchStatusName(name)
        require(dao.watchStatusNameCount(normalizedName, "") == 0) { "观看状态名称已存在" }
        val status = WatchStatusEntity(
            id = newWatchStatusId(),
            name = normalizedName,
            sortOrder = (dao.getWatchStatuses().maxOfOrNull { it.sortOrder } ?: -1) + 1
        )
        dao.insertWatchStatus(status)
        return status
    }

    suspend fun renameWatchStatus(statusId: String, name: String): WatchStatusEntity {
        val normalizedName = normalizeWatchStatusName(name)
        val current = dao.getWatchStatuses().firstOrNull { it.id == statusId }
            ?: throw IllegalArgumentException("观看状态不存在")
        require(dao.watchStatusNameCount(normalizedName, statusId) == 0) { "观看状态名称已存在" }
        val renamed = current.copy(name = normalizedName)
        dao.updateWatchStatus(renamed)
        return renamed
    }

    suspend fun reorderWatchStatuses(statuses: List<WatchStatusEntity>) {
        require(statuses.map { it.id }.distinct().size == statuses.size) { "观看状态不能重复" }
        val existingIds = dao.getWatchStatuses().mapTo(mutableSetOf()) { it.id }
        require(statuses.map { it.id }.toSet() == existingIds) { "必须提供全部观看状态" }
        require(statuses.all { it.id in existingIds }) { "观看状态不存在" }
        dao.updateWatchStatuses(statuses.mapIndexed { index, status -> status.copy(sortOrder = index) })
    }

    suspend fun deleteWatchStatus(statusId: String, destinationStatusId: String? = null) {
        transactionRunner {
            val statuses = dao.getWatchStatuses()
            require(statuses.any { it.id == statusId }) { "观看状态不存在" }
            require(statusId != SYSTEM_WATCHING_ID) { "正在追不能删除" }
            val usedCount = dao.recordCountForStatus(statusId)
            if (usedCount > 0) {
                require(!destinationStatusId.isNullOrBlank()) { "请选择接收记录的观看状态" }
                require(destinationStatusId != statusId) { "接收状态不能与删除状态相同" }
                require(dao.watchStatusExists(destinationStatusId) > 0) { "接收状态不存在" }
                dao.moveRecordsToStatus(statusId, destinationStatusId)
            } else if (destinationStatusId != null) {
                require(destinationStatusId != statusId) { "接收状态不能与删除状态相同" }
                require(dao.watchStatusExists(destinationStatusId) > 0) { "接收状态不存在" }
            }
            dao.deleteWatchStatusById(statusId)
        }
    }

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
        requireNotNull(database).withTransaction {
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

    suspend fun import(
        categories: List<WatchCategoryEntity>,
        records: List<WatchRecordEntity>,
        statuses: List<WatchStatusEntity> = emptyList()
    ) {
        requireNotNull(database).withTransaction {
            ensureBuiltInStatuses()
            val statusIds = importWatchStatuses(statuses)
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
                        val inserted = record.copy(
                            id = 0L,
                            categoryId = mappedCategoryId,
                            title = normalizedTitle,
                            status = statusIds[record.status] ?: SYSTEM_WATCHING_ID,
                            sortOrder = dao.nextRecordSortOrder()
                        )
                        val insertedId = dao.insertRecord(inserted)
                        existingRecords += inserted.copy(id = insertedId)
                    }
                }
            }
        }
    }

    private suspend fun importWatchStatuses(statuses: List<WatchStatusEntity>): Map<String, String> {
        val existing = dao.getWatchStatuses().toMutableList()
        val idMappings = existing.associateTo(mutableMapOf()) { it.id to it.id }
        val nameMappings = existing.associateByTo(mutableMapOf()) { it.name.trim().lowercase() }
        statuses.sortedBy { it.sortOrder }.forEach { source ->
            val normalizedName = normalizeWatchStatusName(source.name)
            val target = existing.firstOrNull { it.id == source.id }
                ?: nameMappings[normalizedName.lowercase()]
                ?: WatchStatusEntity(
                    id = source.id,
                    name = normalizedName,
                    systemType = source.systemType,
                    sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
                ).also {
                    dao.insertWatchStatus(it)
                    existing += it
                    nameMappings[normalizedName.lowercase()] = it
                }
            idMappings[source.id] = target.id
        }
        return idMappings
    }

    companion object {
        val DEFAULT_CATEGORIES = listOf("电视剧", "电影", "动漫", "短剧")

        internal fun normalizeWatchStatusName(name: String): String = name.trim().also {
            require(it.isNotEmpty()) { "观看状态名称不能为空" }
        }

        private fun newWatchStatusId(): String = "CUSTOM_" + java.util.UUID.randomUUID().toString()
    }
}
