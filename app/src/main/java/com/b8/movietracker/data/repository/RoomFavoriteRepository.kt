package com.b8.movietracker.data.repository

import com.b8.movietracker.data.local.MovieEntityMapper
import com.b8.movietracker.data.local.dao.MovieDao
import com.b8.movietracker.data.local.entity.MovieEntity
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.FavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 收藏的 Room 实现（想看/已看 + 观影记录）。
 */
class RoomFavoriteRepository @Inject constructor(
    private val movieDao: MovieDao,
) : FavoriteRepository {

    /** 按状态查询（想看/已看），Room Flow 响应式 */
    override fun getMoviesByStatus(status: Int): Flow<List<Movie>> =
        movieDao.getMoviesByStatus(status).map { entities ->
            entities.map(MovieEntityMapper::toMovie)
        }

    /** 切换「想看」：未收藏则加入想看，已收藏则移除 */
    override suspend fun toggleFavorite(movie: Movie) {
        withContext(Dispatchers.IO) {
            val existing = movieDao.getMovieById(movie.id)
            if (existing == null) {
                movieDao.insert(MovieEntityMapper.toEntity(movie, MovieEntity.STATUS_WANT_TO_WATCH))
            } else {
                movieDao.deleteById(movie.id)
            }
        }
    }

    /** 标记「已看」：写入观影记录（评分/日期/短评/标签），REPLACE 实现更新 */
    override suspend fun markWatched(movie: Movie) {
        withContext(Dispatchers.IO) {
            movieDao.insert(MovieEntityMapper.toEntity(movie, MovieEntity.STATUS_WATCHED))
        }
    }
}
