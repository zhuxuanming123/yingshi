package com.b8.movietracker.domain.repository

import com.b8.movietracker.domain.model.Movie
import kotlinx.coroutines.flow.Flow

/**
 * 影片网络数据源接口（只负责从 TMDB 获取数据）。
 *
 * 收藏相关职责已拆到 FavoriteRepository，这里不再包含，
 * 体现「单一职责」：网络数据源与本地数据源分离。
 *
 * 返回 Flow 的意义：响应式，数据源变化自动推送；异常由调用方 catch 处理。
 */
interface MovieRepository {

    /** 获取热门影片列表 */
    fun getPopularMovies(): Flow<List<Movie>>

    /** 获取影片详情 */
    fun getMovieDetail(movieId: Long): Flow<Movie>

    /** 按关键词搜索影片 */
    fun searchMovies(query: String): Flow<List<Movie>>

    /** 获取影片官方预告片（返回 YouTube 的 video key，无预告片返回 null） */
    fun getMovieTrailer(movieId: Long): Flow<String?>
}
