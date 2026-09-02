package com.example.birthdaycountdown.data

import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WatchlistRepositoryTest {
    @Test
    fun recordRejectsBlankTitle() {
        assertThrows(IllegalArgumentException::class.java) {
            WatchRecordEntity(title = " ", categoryId = 1, currentEpisode = 0)
        }
    }

    @Test
    fun recordRejectsNegativeEpisode() {
        assertThrows(IllegalArgumentException::class.java) {
            WatchRecordEntity(title = "海贼王", categoryId = 1, currentEpisode = -1)
        }
    }

    @Test
    fun addAndRenameStatusTrimAndRejectDuplicates() = runTest {
        val dao = FakeWatchlistDao()
        dao.statuses.value = WatchStatusEntity.builtIns
        val repository = fakeRepository(dao)

        val custom = repository.addWatchStatus("  稍后看  ")
        assertEquals("稍后看", custom.name)
        assertIllegalArgument { repository.addWatchStatus(" 正在追 ") }
        val renamed = repository.renameWatchStatus(custom.id, "  待看  ")
        assertEquals("待看", renamed.name)
        assertIllegalArgument { repository.renameWatchStatus(custom.id, " 已完结 ") }
    }

    @Test
    fun reorderStatusAssignsContiguousOrderAndRejectsDuplicateIds() = runTest {
        val dao = FakeWatchlistDao()
        dao.statuses.value = WatchStatusEntity.builtIns
        val repository = fakeRepository(dao)
        val reordered = dao.statuses.value.reversed()

        repository.reorderWatchStatuses(reordered)

        assertEquals(reordered.map { it.id }, dao.statuses.value.sortedBy { it.sortOrder }.map { it.id })
        assertIllegalArgument { repository.reorderWatchStatuses(listOf(reordered[0], reordered[0])) }
    }

    @Test
    fun ensureBuiltInStatusesAddsOnlyMissingIds() = runTest {
        val dao = FakeWatchlistDao()
        dao.statuses.value = listOf(WatchStatusEntity.builtIns.first())
        val repository = fakeRepository(dao)

        repository.ensureBuiltInStatuses()

        assertEquals(WatchStatusEntity.builtIns.map { it.id }, dao.statuses.value.sortedBy { it.sortOrder }.map { it.id })
    }

    @Test
    fun deleteUsedStatusRequiresDifferentDestinationAndMigratesRecords() = runTest {
        val dao = FakeWatchlistDao()
        dao.statuses.value = WatchStatusEntity.builtIns
        dao.addRecord(WatchRecordEntity(id = 1, title = "剧集", categoryId = 1, currentEpisode = 2, status = "PAUSED"))
        val repository = fakeRepository(dao)

        assertIllegalArgument { repository.deleteWatchStatus("PAUSED") }
        assertIllegalArgument { repository.deleteWatchStatus("PAUSED", "PAUSED") }
        repository.deleteWatchStatus("PAUSED", "COMPLETED")

        assertEquals("COMPLETED", dao.records.single().status)
        assertEquals(false, dao.statuses.value.any { it.id == "PAUSED" })
    }

    @Test
    fun deleteWatchingIsProtectedAndUnusedCustomStatusNeedsNoDestination() = runTest {
        val dao = FakeWatchlistDao()
        dao.statuses.value = WatchStatusEntity.builtIns
        val repository = fakeRepository(dao)
        val custom = repository.addWatchStatus("重看")

        assertIllegalArgument { repository.deleteWatchStatus(SYSTEM_WATCHING_ID) }
        repository.deleteWatchStatus(custom.id)

        assertEquals(false, dao.statuses.value.any { it.id == custom.id })
    }

    private suspend fun assertIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun fakeRepository(dao: WatchlistDao) = WatchlistRepository(
        database = null,
        dao = dao,
        transactionRunner = { block -> block() }
    )

    private class FakeWatchlistDao : WatchlistDao {
        val statuses = MutableStateFlow<List<WatchStatusEntity>>(emptyList())
        private val categories = mutableListOf<WatchCategoryEntity>()
        val records = mutableListOf<WatchRecordEntity>()

        fun addRecord(record: WatchRecordEntity) {
            records += record
        }

        override fun observeWatchStatuses(): Flow<List<WatchStatusEntity>> = statuses
        override suspend fun getWatchStatuses() = statuses.value
        override fun observeCategories(): Flow<List<WatchCategoryEntity>> = MutableStateFlow(categories)
        override fun observeRecords(): Flow<List<WatchRecordEntity>> = MutableStateFlow(records)
        override suspend fun getCategories() = categories.toList()
        override suspend fun getRecords() = records.toList()
        override suspend fun insertCategory(category: WatchCategoryEntity) = category.id
        override suspend fun insertRecord(record: WatchRecordEntity) = record.id
        override suspend fun insertWatchStatus(status: WatchStatusEntity) { statuses.value = statuses.value + status }
        override suspend fun updateCategory(category: WatchCategoryEntity) = Unit
        override suspend fun updateRecord(record: WatchRecordEntity) = Unit
        override suspend fun updateWatchStatus(status: WatchStatusEntity) { statuses.value = statuses.value.map { if (it.id == status.id) status else it } }
        override suspend fun updateWatchStatuses(statuses: List<WatchStatusEntity>) {
            val updates = statuses.associateBy { it.id }
            this.statuses.value = this.statuses.value.map { updates[it.id] ?: it }
        }
        override suspend fun adjustEpisode(recordId: Long, delta: Int, lastWatchedAt: Long) = Unit
        override suspend fun updateRecords(records: List<WatchRecordEntity>) = Unit
        override suspend fun updateCategories(categories: List<WatchCategoryEntity>) = Unit
        override suspend fun deleteRecord(record: WatchRecordEntity) = Unit
        override suspend fun deleteWatchStatusById(statusId: String) { statuses.value = statuses.value.filterNot { it.id == statusId } }
        override suspend fun deleteCategoryById(categoryId: Long) = Unit
        override suspend fun nextCategorySortOrder() = categories.size
        override suspend fun nextRecordSortOrder() = records.size
        override suspend fun categoryCount() = categories.size
        override suspend fun watchStatusCount() = statuses.value.size
        override suspend fun watchStatusExists(statusId: String) = if (statuses.value.any { it.id == statusId }) 1 else 0
        override suspend fun watchStatusNameCount(name: String, exceptId: String) = statuses.value.count { it.id != exceptId && it.name.equals(name, ignoreCase = true) }
        override suspend fun categoryExists(categoryId: Long) = 0
        override suspend fun categoryNameCount(name: String, exceptId: Long) = 0
        override suspend fun findCategoryIdByName(name: String): Long? = null
        override suspend fun recordCountForCategory(categoryId: Long) = 0
        override suspend fun recordCountForStatus(statusId: String) = records.count { it.status == statusId }
        override suspend fun moveRecordsToStatus(sourceStatusId: String, targetStatusId: String) {
            records.replaceAll { if (it.status == sourceStatusId) it.copy(status = targetStatusId) else it }
        }
        override suspend fun updateRecordStatus(recordId: Long, statusId: String) = Unit
        override suspend fun moveRecords(sourceCategoryId: Long, targetCategoryId: Long) = Unit
    }
}
