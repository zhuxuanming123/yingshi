package com.b8.movietracker.data.local

import com.b8.movietracker.data.local.entity.MovieEntity
import com.b8.movietracker.domain.model.Movie

/**
 * 数据库实体 ↔ 领域模型 映射器。
 *
 * 为什么需要映射？
 *   MovieEntity（数据库）与 Movie（业务）字段语义不同：
 *   - Entity 有 watchStatus（片单类型）、addedAt（加入时间），Movie 没有
 *   - Movie 有 isFavorite（收藏态），Entity 用 watchStatus 表达
 *   两者解耦，各层独立演化。
 *
 * 用 object 单例：纯函数、无状态，无需依赖注入。
 */
object MovieEntityMapper {

    /** 数据库实体 → 领域模型（从 Room 读出，交给 UI 展示） */
    fun toMovie(entity: MovieEntity): Movie = Movie(
        id = entity.id,
        title = entity.title,
        posterUrl = entity.posterUrl,
        overview = entity.overview,
        releaseDate = entity.releaseDate,
        voteAverage = entity.voteAverage,
        isFavorite = true, // 存在于数据库中的都是已收藏的影片
    )

    /**
     * 领域模型 → 数据库实体（收藏时写入 Room）。
     * @param watchStatus 片单类型，默认「想看」
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
        addedAt = System.currentTimeMillis(), // 加入时间戳
    )
}
