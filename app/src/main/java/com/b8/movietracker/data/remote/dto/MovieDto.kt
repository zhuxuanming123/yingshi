package com.b8.movietracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 影片 DTO（Data Transfer Object，网络层数据传输对象）。
 *
 * 为什么需要 DTO？
 *   网络返回的 JSON 字段命名是「蛇形」(poster_path)，
 *   且字段与业务需求不完全一致，因此单独定义一层，与领域模型解耦。
 *
 * @SerializedName 作用：
 *   Gson 默认按 Kotlin 属性名匹配 JSON 字段，字段名不同时需手动指定映射。
 *
 * 关于可空性：
 *   TMDB 对 poster_path / overview 等可能返回 null 或空串，
 *   故声明为 String? 防止反序列化崩溃。
 */
data class MovieDto(
    /** 影片 ID */
    @SerializedName("id") val id: Long,

    /** 标题 */
    @SerializedName("title") val title: String,

    /** 海报图相对路径（如 /abc.jpg），需拼接 baseUrl 才是完整地址 */
    @SerializedName("poster_path") val posterPath: String?,

    /** 背景大图相对路径 */
    @SerializedName("backdrop_path") val backdropPath: String?,

    /** 简介 */
    @SerializedName("overview") val overview: String?,

    /** 上映日期（格式 yyyy-MM-dd） */
    @SerializedName("release_date") val releaseDate: String?,

    /** 平均评分（0~10） */
    @SerializedName("vote_average") val voteAverage: Double,

    /** 评分人数 */
    @SerializedName("vote_count") val voteCount: Int,

    /** 类型 ID 列表 */
    @SerializedName("genre_ids") val genreIds: List<Int> = emptyList(),

    /** 原始语言（如 en、ja） */
    @SerializedName("original_language") val originalLanguage: String?,

    /** 热度值 */
    @SerializedName("popularity") val popularity: Double,

    /** 是否成人内容 */
    @SerializedName("adult") val adult: Boolean,
)
