package com.b8.movietracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 影片本地数据库实体（任务1：扩展为「观影记录表」）。
 *
 * 在原有片单字段基础上，新增观影记录相关字段：
 *   - userRating：用户评分（1-10，标记"已看"时填写）
 *   - watchedDate：观影日期时间戳
 *   - review：短评
 *   - tags：自定义标签（逗号分隔）
 *   - genreIds：影片类型 id（逗号分隔，用于类型分布统计）
 *
 * watchStatus 区分「想看(0)」与「已看(1)」。
 */
@Entity(tableName = "movies")
data class MovieEntity(
    /** 影片 ID，主键 */
    @PrimaryKey val id: Long,

    /** 标题 */
    val title: String,

    /** 海报完整地址 */
    val posterUrl: String?,

    /** 简介 */
    val overview: String?,

    /** 上映日期 */
    val releaseDate: String?,

    /** 评分（TMDB 平均分） */
    val voteAverage: Double,

    /** 片单类型：STATUS_WANT_TO_WATCH 或 STATUS_WATCHED */
    val watchStatus: Int,

    /** 加入片单的时间戳 */
    val addedAt: Long,

    /** 用户评分（1-10，标记已看时填写） */
    val userRating: Double? = null,

    /** 观影日期时间戳（标记已看时填写） */
    val watchedDate: Long? = null,

    /** 短评 */
    val review: String? = null,

    /** 自定义标签（逗号分隔） */
    val tags: String? = null,

    /** 影片类型 id（逗号分隔，用于统计） */
    val genreIds: String? = null,
) {
    companion object {
        /** 想看 */
        const val STATUS_WANT_TO_WATCH = 0

        /** 已看 */
        const val STATUS_WATCHED = 1
    }
}
