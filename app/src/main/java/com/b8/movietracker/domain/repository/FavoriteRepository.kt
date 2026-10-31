package com.b8.movietracker.domain.repository

import com.b8.movietracker.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * 本地收藏数据源接口（想看/已看）。
 */
interface FavoriteRepository {

    /** 按收藏状态查询影片列表（想看/已看） */
    fun getMoviesByStatus(status: Int): Flow<List<Movie>>

    /** 切换「想看」：未收藏则加入想看，已收藏则移除 */
    suspend fun toggleFavorite(movie: Movie)

    /** 标记「已看」：写入观影记录（评分/日期/短评/标签），主键冲突则更新 */
    suspend fun markWatched(movie: Movie)

    companion object {
        /** 想看 */
        const val STATUS_WANT_TO_WATCH = 0

        /** 已看 */
        const val STATUS_WATCHED = 1
    }
}
