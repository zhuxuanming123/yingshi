package com.b8.movietracker.domain.model

/**
 * 影片领域模型。
 *
 * 基础字段：id / title / posterUrl / overview / releaseDate / voteAverage / isFavorite
 * 观影记录字段（标记已看时填写）：userRating / watchedDate / review / tags
 * 类型字段（用于类型分布统计）：genreIds
 */
data class Movie(
    val id: Long,
    val title: String,
    val posterUrl: String?,
    val overview: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val isFavorite: Boolean = false,

    /** 收藏状态：null=未收藏，0=想看，1=已看 */
    val watchStatus: Int? = null,

    /** 影片类型 id 列表（来自 TMDB） */
    val genreIds: List<Int> = emptyList(),

    /** 用户评分（1-10） */
    val userRating: Double? = null,

    /** 观影日期时间戳 */
    val watchedDate: Long? = null,

    /** 短评 */
    val review: String? = null,

    /** 自定义标签 */
    val tags: List<String> = emptyList(),
)
