package com.b8.movietracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * TMDB 类型字典接口 /genre/movie/list 的响应。
 * 返回 { "genres": [ { "id": 28, "name": "动作" }, ... ] }
 */
data class GenreListResponseDto(
    @SerializedName("genres") val genres: List<GenreDto> = emptyList(),
)
