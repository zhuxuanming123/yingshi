package com.b8.movietracker.data.repository

import com.b8.movietracker.data.local.dao.SearchHistoryDao
import com.b8.movietracker.data.local.entity.SearchHistoryEntity
import com.b8.movietracker.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 搜索历史的 Room 实现（任务3）。
 */
class RoomSearchHistoryRepository @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao,
) : SearchHistoryRepository {

    override fun getSearchHistory(): Flow<List<String>> =
        searchHistoryDao.getRecentHistory().map { list ->
            list.map { it.keyword }
        }

    override suspend fun addSearchHistory(keyword: String) {
        searchHistoryDao.insert(
            SearchHistoryEntity(keyword = keyword, timestamp = System.currentTimeMillis())
        )
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.clearAll()
    }
}
