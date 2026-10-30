package com.b8.movietracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.b8.movietracker.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 搜索历史 DAO（任务3）。
 */
@Dao
interface SearchHistoryDao {

    /** 插入/更新搜索记录（同关键词用 REPLACE 更新时间戳） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchHistoryEntity)

    /** 查询最近 10 条搜索历史，按时间倒序 */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentHistory(): Flow<List<SearchHistoryEntity>>

    /** 清空全部搜索历史 */
    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
