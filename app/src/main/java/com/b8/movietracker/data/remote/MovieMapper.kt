package com.b8.movietracker.data.remote

import com.b8.movietracker.data.remote.dto.MovieDetailDto
import com.b8.movietracker.data.remote.dto.MovieDto
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.util.ImageUrlBuilder

/**
 * DTO → 领域模型 映射器（任务3）。
 *
 * 为什么要这一层？
 *   网络 DTO 字段与业务模型不一致，典型差异：
 *   1. 字段名：poster_path（蛇形） vs posterUrl（驼峰）
 *   2. 数据形态：posterPath 是相对路径，posterUrl 是拼接好的完整地址
 *   3. 业务字段：isFavorite 是本地状态，网络里没有，这里默认 false
 *
 * 用 object 单例即可：纯函数、无状态，不需要依赖注入。
 */
object MovieMapper {

    /** 列表项 DTO → Movie（热门/搜索共用的列表结构） */
    fun toMovie(dto: MovieDto): Movie = Movie(
        id = dto.id,
        title = dto.title,
        posterUrl = ImageUrlBuilder.buildPosterUrl(dto.posterPath),
        overview = dto.overview,
        releaseDate = dto.releaseDate,
        voteAverage = dto.voteAverage,
        isFavorite = false, // 网络数据不含收藏态，默认 false，由详情页组合本地收藏判断
    )

    /** 详情 DTO → Movie（详情接口字段更全，但映射到 Movie 只取所需字段） */
    fun toMovie(dto: MovieDetailDto): Movie = Movie(
        id = dto.id,
        title = dto.title,
        posterUrl = ImageUrlBuilder.buildPosterUrl(dto.posterPath),
        overview = dto.overview,
        releaseDate = dto.releaseDate,
        voteAverage = dto.voteAverage,
        isFavorite = false,
    )
}
