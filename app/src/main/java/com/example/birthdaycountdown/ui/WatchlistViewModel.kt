package com.example.birthdaycountdown.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.birthdaycountdown.data.WatchCategoryEntity
import com.example.birthdaycountdown.data.WatchRecordEntity
import com.example.birthdaycountdown.data.WatchStatusEntity
import com.example.birthdaycountdown.data.WatchlistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchlistViewModel(private val repository: WatchlistRepository) : ViewModel() {
    val categories: StateFlow<List<WatchCategoryEntity>> = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val records: StateFlow<List<WatchRecordEntity>> = repository.records.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val watchStatuses: StateFlow<List<WatchStatusEntity>> = repository.watchStatuses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
            repository.ensureBuiltInStatuses()
        }
    }

    fun saveRecord(record: WatchRecordEntity) = viewModelScope.launch { repository.saveRecord(record) }
    fun deleteRecord(record: WatchRecordEntity, onDeleted: () -> Unit = {}) = viewModelScope.launch {
        repository.deleteRecord(record)
        onDeleted()
    }
    fun adjustEpisode(record: WatchRecordEntity, delta: Int) = viewModelScope.launch { repository.adjustEpisode(record.id, delta) }
    fun reorderRecords(records: List<WatchRecordEntity>) = viewModelScope.launch { repository.reorderRecords(records) }
    fun reorderCategories(categories: List<WatchCategoryEntity>) = viewModelScope.launch { repository.reorderCategories(categories) }
    fun reorderWatchStatuses(statuses: List<WatchStatusEntity>) = viewModelScope.launch { repository.reorderWatchStatuses(statuses) }

    fun addWatchStatus(name: String, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { repository.addWatchStatus(name) }.onFailure { onError(it.message ?: "添加观看状态失败") }
    }

    fun renameWatchStatus(statusId: String, name: String, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { repository.renameWatchStatus(statusId, name) }.onFailure { onError(it.message ?: "保存观看状态失败") }
    }

    fun deleteWatchStatus(statusId: String, destinationStatusId: String?, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { repository.deleteWatchStatus(statusId, destinationStatusId) }.onFailure { onError(it.message ?: "删除观看状态失败") }
    }

    fun saveCategory(category: WatchCategoryEntity, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { repository.saveCategory(category) }.onFailure { onError(it.message ?: "保存分类失败") }
    }

    fun deleteCategory(categoryId: Long, targetCategoryId: Long?, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { repository.deleteCategoryAndMoveRecords(categoryId, targetCategoryId) }.onFailure { onError(it.message ?: "删除分类失败") }
    }
}
