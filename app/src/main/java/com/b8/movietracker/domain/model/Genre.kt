package com.b8.movietracker.domain.model

/**
 * 影片类型（如 动作/喜剧/科幻）。
 * 来源：TMDB 类型字典接口 /genre/movie/list。
 */
data class Genre(
    val id: Int,
    val name: String,
)
