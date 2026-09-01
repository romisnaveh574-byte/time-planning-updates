package com.example.birthdaycountdown.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdown_records ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdown_records ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<CountdownEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CountdownEntity): Long

    @Update suspend fun update(record: CountdownEntity)
    @Update suspend fun updateAll(records: List<CountdownEntity>)
    @Delete suspend fun delete(record: CountdownEntity)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM countdown_records")
    suspend fun nextSortOrder(): Int

    @Query("SELECT * FROM countdown_records WHERE type = 'BIRTHDAY' LIMIT 1")
    suspend fun findBirthday(): CountdownEntity?

    @Query("SELECT * FROM countdown_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CountdownEntity?
}
