package com.b8.movietracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 搜索历史实体（任务3：本地 Room 存储搜索关键词与时间）。
 *
 * 设计说明：
 *   - keyword 作为主键（同一关键词只保留一条，重复搜索更新时间为最新）
 *   - timestamp 记录最近搜索时间，用于"最近10条"排序
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    /** 搜索关键词（主键，去重） */
    @PrimaryKey val keyword: String,

    /** 搜索时间戳（毫秒） */
    val timestamp: Long,
)
