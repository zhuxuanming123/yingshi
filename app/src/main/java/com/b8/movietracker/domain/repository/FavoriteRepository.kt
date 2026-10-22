package com.b8.movietracker.domain.repository

import com.b8.movietracker.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * 本地收藏数据源接口（想看/已看）。
 *
 * 为什么从 MovieRepository 里拆出来？
 *   「网络数据」和「本地收藏」是两个完全不同来源的数据，
 *   拆开后各自职责单一，且阶段6 用 Room 替换时只改这一个接口的实现。
 *
 * 当前实现为 RoomFavoriteRepository（Room 持久化，重启 App 数据不丢失）。
 */
interface FavoriteRepository {

    /** 收藏的影片列表（响应式，切换收藏后自动刷新） */
    fun getFavoriteMovies(): Flow<List<Movie>>

    /** 切换某部影片的收藏状态（传入完整 Movie，便于本地存储） */
    suspend fun toggleFavorite(movie: Movie)
}
