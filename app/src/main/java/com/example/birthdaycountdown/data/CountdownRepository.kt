package com.example.birthdaycountdown.data

import kotlinx.coroutines.flow.Flow

class CountdownRepository(private val dao: CountdownDao) {
    val records: Flow<List<CountdownEntity>> = dao.observeAll()

    suspend fun allRecords(): List<CountdownEntity> = dao.getAll()

    suspend fun save(record: CountdownEntity): Long {
        return if (record.id == 0L) dao.insert(record.copy(sortOrder = dao.nextSortOrder())) else { dao.update(record); record.id }
    }

    suspend fun delete(record: CountdownEntity) = dao.delete(record)

    suspend fun reorder(records: List<CountdownEntity>) {
        dao.updateAll(records.mapIndexed { index, record -> record.copy(sortOrder = index) })
    }

    suspend fun import(records: List<CountdownEntity>): List<CountdownEntity> {
        val existing = dao.getAll().toMutableList()
        return records.filterNot { incoming -> existing.any { it.sameImportedRecord(incoming) } }.map { record ->
            val id = save(record.copy(id = 0L))
            record.copy(id = id).also { existing += it }
        }
    }

    private fun CountdownEntity.sameImportedRecord(other: CountdownEntity): Boolean =
        type == other.type && name.trim().equals(other.name.trim(), ignoreCase = true) &&
            dateTimeIso == other.dateTimeIso && calendarType == other.calendarType &&
            lunarYear == other.lunarYear && lunarMonth == other.lunarMonth &&
            lunarDay == other.lunarDay && lunarLeapMonth == other.lunarLeapMonth
}
