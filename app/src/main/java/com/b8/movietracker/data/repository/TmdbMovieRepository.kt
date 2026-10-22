package com.b8.movietracker.data.repository

import com.b8.movietracker.data.remote.MovieMapper
import com.b8.movietracker.data.remote.TmdbApiService
import com.b8.movietracker.data.remote.safeApiCall
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * TMDB 网络数据仓库（任务1/4：对接真实 API，替换假数据）。
 *
 * 设计要点：
 *   - 用 safeApiCall 统一捕获异常，网络失败会抛出带友好提示的 ApiException，
 *     由 ViewModel 的 catch 转为 Error 状态展示（任务5）。
 *   - .flowOn(Dispatchers.IO)：整个 flow 在 IO 线程执行，绝不阻塞主线程。
 *   - 通过 MovieMapper 把 DTO 映射成领域模型 Movie（任务3）。
 */
class TmdbMovieRepository @Inject constructor(
    private val apiService: TmdbApiService,
) : MovieRepository {

    /** 热门电影（本阶段拉第 1 页，阶段3 做分页加载） */
    override fun getPopularMovies(): Flow<List<Movie>> = flow {
        val response = safeApiCall { apiService.getPopularMovies(page = 1) }
        emit(response.results.map(MovieMapper::toMovie))
    }.flowOn(Dispatchers.IO)

    /** 电影详情 */
    override fun getMovieDetail(movieId: Long): Flow<Movie> = flow {
        val detail = safeApiCall { apiService.getMovieDetail(movieId) }
        emit(MovieMapper.toMovie(detail))
    }.flowOn(Dispatchers.IO)

    /** 搜索电影 */
    override fun searchMovies(query: String): Flow<List<Movie>> = flow {
        val response = safeApiCall { apiService.searchMovies(query = query) }
        emit(response.results.map(MovieMapper::toMovie))
    }.flowOn(Dispatchers.IO)
}
