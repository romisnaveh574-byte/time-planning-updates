package com.example.birthdaycountdown.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class WatchStatus(val label: String) {
    WATCHING("正在追"),
    COMPLETED("已完结"),
    PAUSED("搁置"),
    DROPPED("弃剧"),
    ARCHIVED("归档")
}

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
    val totalEpisodes: Int? = null,
    val platform: String = "",
    val status: String = SYSTEM_WATCHING_ID,
    val lastWatchedAt: Long = 0L,
    val sortOrder: Int = Int.MAX_VALUE
) {
    @Deprecated("Use the stable string status ID instead")
    constructor(
        id: Long = 0,
        title: String,
        categoryId: Long,
        currentEpisode: Int,
        totalEpisodes: Int? = null,
        platform: String = "",
        status: WatchStatus,
        lastWatchedAt: Long = 0L,
        sortOrder: Int = Int.MAX_VALUE
    ) : this(
        id = id,
        title = title,
        categoryId = categoryId,
        currentEpisode = currentEpisode,
        totalEpisodes = totalEpisodes,
        platform = platform,
        status = status.name,
        lastWatchedAt = lastWatchedAt,
        sortOrder = sortOrder
    )

    init {
        require(title.trim().isNotEmpty()) { "剧名不能为空" }
        require(categoryId > 0) { "分类无效" }
        require(currentEpisode >= 0) { "集数不能小于 0" }
        require(totalEpisodes == null || totalEpisodes >= currentEpisode) { "总集数不能小于当前集数" }
    }
}

const val SYSTEM_WATCHING_ID = "WATCHING"
