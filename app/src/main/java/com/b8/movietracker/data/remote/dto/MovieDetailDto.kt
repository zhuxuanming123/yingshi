package com.b8.movietracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 影片详情 DTO（阶段4 详情页使用，阶段0 先定义好接口对应结构）。
 *
 * 详情接口比列表接口多出时长、类型、标语、状态等字段。
 */
data class MovieDetailDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,

    /** 平均评分 */
    @SerializedName("vote_average") val voteAverage: Double,

    /** 评分人数 */
    @SerializedName("vote_count") val voteCount: Int,

    /** 片长（分钟） */
    @SerializedName("runtime") val runtime: Int,

    /** 标语（tagline） */
    @SerializedName("tagline") val tagline: String?,

    /** 类型列表（详情接口返回完整类型对象，与列表接口的 genre_ids 不同） */
    @SerializedName("genres") val genres: List<GenreDto> = emptyList(),

    /** 影片状态（Released / In Production 等） */
    @SerializedName("status") val status: String?,
)

/** 类型对象 */
data class GenreDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
)
