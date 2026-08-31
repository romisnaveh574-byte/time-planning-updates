package com.example.birthdaycountdown.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watch_categories ORDER BY sortOrder ASC, id ASC")
    fun observeCategories(): Flow<List<WatchCategoryEntity>>

    @Query("SELECT * FROM watch_records ORDER BY sortOrder ASC, id ASC")
    fun observeRecords(): Flow<List<WatchRecordEntity>>

    @Query("SELECT * FROM watch_categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getCategories(): List<WatchCategoryEntity>

    @Query("SELECT * FROM watch_records ORDER BY sortOrder ASC, id ASC")
    suspend fun getRecords(): List<WatchRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: WatchCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WatchRecordEntity): Long

    @Update
    suspend fun updateCategory(category: WatchCategoryEntity)

    @Update
    suspend fun updateRecord(record: WatchRecordEntity)

    @Query("UPDATE watch_records SET currentEpisode = MAX(0, currentEpisode + :delta) WHERE id = :recordId")
    suspend fun adjustEpisode(recordId: Long, delta: Int)

    @Update
    suspend fun updateRecords(records: List<WatchRecordEntity>)

    @Update
    suspend fun updateCategories(categories: List<WatchCategoryEntity>)

    @Delete
    suspend fun deleteRecord(record: WatchRecordEntity)

    @Query("DELETE FROM watch_categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM watch_categories")
    suspend fun nextCategorySortOrder(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM watch_records")
    suspend fun nextRecordSortOrder(): Int

    @Query("SELECT COUNT(*) FROM watch_categories")
    suspend fun categoryCount(): Int

    @Query("SELECT COUNT(*) FROM watch_categories WHERE id = :categoryId")
    suspend fun categoryExists(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM watch_categories WHERE lower(name) = lower(:name) AND id != :exceptId")
    suspend fun categoryNameCount(name: String, exceptId: Long): Int

    @Query("SELECT id FROM watch_categories WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun findCategoryIdByName(name: String): Long?

    @Query("SELECT COUNT(*) FROM watch_records WHERE categoryId = :categoryId")
    suspend fun recordCountForCategory(categoryId: Long): Int

    @Query("UPDATE watch_records SET categoryId = :targetCategoryId WHERE categoryId = :sourceCategoryId")
    suspend fun moveRecords(sourceCategoryId: Long, targetCategoryId: Long)
}
