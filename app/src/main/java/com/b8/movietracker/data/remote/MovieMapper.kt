package com.b8.movietracker.data.remote

import com.b8.movietracker.data.remote.dto.MovieDetailDto
import com.b8.movietracker.data.remote.dto.MovieDto
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.util.ImageUrlBuilder

/**
 * DTO → 领域模型 映射器。
 */
object MovieMapper {

    /** 列表项 DTO → Movie（热门/搜索/发现共用的列表结构） */
    fun toMovie(dto: MovieDto): Movie = Movie(
        id = dto.id,
        title = dto.title,
        posterUrl = ImageUrlBuilder.buildPosterUrl(dto.posterPath),
        overview = dto.overview,
        releaseDate = dto.releaseDate,
        voteAverage = dto.voteAverage,
        isFavorite = false,
        genreIds = dto.genreIds, // 列表接口返回 genre_ids
    )

    /** 详情 DTO → Movie（详情接口的 genres 是完整类型对象，取 id） */
    fun toMovie(dto: MovieDetailDto): Movie = Movie(
        id = dto.id,
        title = dto.title,
        posterUrl = ImageUrlBuilder.buildPosterUrl(dto.posterPath),
        overview = dto.overview,
        releaseDate = dto.releaseDate,
        voteAverage = dto.voteAverage,
        isFavorite = false,
        genreIds = dto.genres.map { it.id },
    )
}
