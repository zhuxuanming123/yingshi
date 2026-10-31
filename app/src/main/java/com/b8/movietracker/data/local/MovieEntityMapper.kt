package com.b8.movietracker.data.local

import com.b8.movietracker.data.local.entity.MovieEntity
import com.b8.movietracker.domain.model.Movie

/**
 * 数据库实体 ↔ 领域模型 映射器。
 *
 * 处理 Entity 与 Movie 的字段差异：
 *   - Entity 的 genreIds/tags 是逗号分隔字符串，Movie 是 List
 *   - Entity 有 watchStatus/addedAt，Movie 用 isFavorite 表达收藏态
 */
object MovieEntityMapper {

    /** 数据库实体 → 领域模型 */
    fun toMovie(entity: MovieEntity): Movie = Movie(
        id = entity.id,
        title = entity.title,
        posterUrl = entity.posterUrl,
        overview = entity.overview,
        releaseDate = entity.releaseDate,
        voteAverage = entity.voteAverage,
        isFavorite = true, // 存在于数据库中的都是已收藏的影片
        watchStatus = entity.watchStatus, // 收藏状态（想看/已看）
        genreIds = entity.genreIds
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?: emptyList(),
        userRating = entity.userRating,
        watchedDate = entity.watchedDate,
        review = entity.review,
        tags = entity.tags
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
    )

    /**
     * 领域模型 → 数据库实体。
     * @param watchStatus 片单类型（想看/已看）
     */
    fun toEntity(
        movie: Movie,
        watchStatus: Int = MovieEntity.STATUS_WANT_TO_WATCH,
    ): MovieEntity = MovieEntity(
        id = movie.id,
        title = movie.title,
        posterUrl = movie.posterUrl,
        overview = movie.overview,
        releaseDate = movie.releaseDate,
        voteAverage = movie.voteAverage,
        watchStatus = watchStatus,
        addedAt = System.currentTimeMillis(),
        userRating = movie.userRating,
        watchedDate = movie.watchedDate,
        review = movie.review,
        tags = if (movie.tags.isEmpty()) null else movie.tags.joinToString(","),
        genreIds = if (movie.genreIds.isEmpty()) null else movie.genreIds.joinToString(","),
    )
}
