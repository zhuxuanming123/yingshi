package com.b8.movietracker.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 搜索历史数据源接口（任务3）。
 */
interface SearchHistoryRepository {

    /** 获取最近搜索历史（关键词列表，按时间倒序，最多 10 条） */
    fun getSearchHistory(): Flow<List<String>>

    /** 记录一次搜索关键词 */
    suspend fun addSearchHistory(keyword: String)

    /** 清空全部搜索历史 */
    suspend fun clearSearchHistory()
}
