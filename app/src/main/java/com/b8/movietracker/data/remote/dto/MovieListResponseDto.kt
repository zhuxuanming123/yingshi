package com.b8.movietracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 影片列表接口的统一响应 DTO。
 *
 * TMDB 列表接口（popular / top_rated / trending）返回结构一致：
 * {
 *   "page": 1,
 *   "results": [ ...影片数组... ],
 *   "total_pages": 100,
 *   "total_results": 2000
 * }
 *
 * 分页字段 totalPages / totalResults 阶段3 做分页加载时使用。
 */
data class MovieListResponseDto(
    /** 当前页码 */
    @SerializedName("page") val page: Int,

    /** 本页影片列表 */
    @SerializedName("results") val results: List<MovieDto>,

    /** 总页数 */
    @SerializedName("total_pages") val totalPages: Int,

    /** 总条数 */
    @SerializedName("total_results") val totalResults: Int,
)
