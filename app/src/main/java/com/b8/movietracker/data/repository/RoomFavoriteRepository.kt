package com.b8.movietracker.data.repository

import com.b8.movietracker.data.local.MovieEntityMapper
import com.b8.movietracker.data.local.dao.MovieDao
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.FavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Room 本地收藏仓库（任务3：收藏写入本地数据库，我的片单从 Room 读取）。
 *
 * 替换阶段2 的 InMemoryFavoriteRepository，实现「重启 App 数据不丢失」。
 *
 * 线程策略（任务4）：
 *   - getFavoriteMovies 返回 Room 的 Flow，Room 内部线程池自动处理，不阻塞主线程；
 *   - toggleFavorite 里的 DAO 调用是 suspend，但仍显式用 withContext(Dispatchers.IO)
 *     包裹，确保数据库 IO 明确落在 IO 调度器上。
 */
class RoomFavoriteRepository @Inject constructor(
    private val movieDao: MovieDao,
) : FavoriteRepository {

    /** 从 Room 读取全部片单，映射成领域模型（响应式，收藏变化自动刷新） */
    override fun getFavoriteMovies(): Flow<List<Movie>> =
        movieDao.getAllMovies().map { entities ->
            entities.map(MovieEntityMapper::toMovie)
        }

    /** 切换收藏：未收藏则写入数据库，已收藏则删除 */
    override suspend fun toggleFavorite(movie: Movie) {
        withContext(Dispatchers.IO) {
            val existing = movieDao.getMovieById(movie.id)
            if (existing == null) {
                // 未收藏 → 写入数据库
                movieDao.insert(MovieEntityMapper.toEntity(movie))
            } else {
                // 已收藏 → 从数据库删除
                movieDao.deleteById(movie.id)
            }
        }
    }
}
