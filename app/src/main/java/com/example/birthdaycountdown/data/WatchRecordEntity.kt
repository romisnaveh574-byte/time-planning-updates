package com.example.birthdaycountdown.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_records",
    foreignKeys = [ForeignKey(
        entity = WatchCategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.NO_ACTION
    )],
    indices = [Index("categoryId")]
)
data class WatchRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val categoryId: Long,
    val currentEpisode: Int,
    val sortOrder: Int = Int.MAX_VALUE
) {
    init {
        require(title.trim().isNotEmpty()) { "剧名不能为空" }
        require(categoryId > 0) { "分类无效" }
        require(currentEpisode >= 0) { "集数不能小于 0" }
    }
}
