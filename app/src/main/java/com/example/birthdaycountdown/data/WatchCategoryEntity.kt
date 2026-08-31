package com.example.birthdaycountdown.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_categories")
data class WatchCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = Int.MAX_VALUE
) {
    init {
        require(name.trim().isNotEmpty()) { "分类名称不能为空" }
    }
}
