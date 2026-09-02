package com.example.birthdaycountdown.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_statuses")
data class WatchStatusEntity(
    @PrimaryKey val id: String,
    val name: String,
    val systemType: String? = null,
    val sortOrder: Int
) {
    companion object {
        val builtIns: List<WatchStatusEntity> = listOf(
            WatchStatusEntity("WATCHING", "正在追", "WATCHING", 0),
            WatchStatusEntity("COMPLETED", "已完结", "COMPLETED", 1),
            WatchStatusEntity("PAUSED", "搁置", "PAUSED", 2),
            WatchStatusEntity("DROPPED", "弃剧", "DROPPED", 3),
            WatchStatusEntity("ARCHIVED", "归档", "ARCHIVED", 4)
        )
    }
}
