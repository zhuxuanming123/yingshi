package com.b8.movietracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 影片本地数据库实体（任务1：Room Entity，保存「想看/已看」片单）。
 *
 * 字段对应任务要求：
 *   id / title / posterUrl(海报) / overview(简介) / releaseDate(上映时间) /
 *   voteAverage(评分) / watchStatus(片单类型：想看/已看)
 *
 * 设计说明：
 *   - watchStatus 一个字段区分「想看(0)」与「已看(1)」，查询时按它过滤即可得到对应片单。
 *   - addedAt 记录加入时间，用于列表按加入时间倒序展示。
 *   - posterUrl 存完整海报地址（阶段3 起改存完整 URL，与领域模型 Movie 直接对应，免去拼接/还原）。
 *
 * @Entity(tableName = "movies") 指定表名。
 */
@Entity(tableName = "movies")
data class MovieEntity(
    /** 影片 ID，主键（与 TMDB 影片 ID 一一对应） */
    @PrimaryKey val id: Long,

    /** 标题 */
    val title: String,

    /** 海报完整地址 */
    val posterUrl: String?,

    /** 简介 */
    val overview: String?,

    /** 上映日期 */
    val releaseDate: String?,

    /** 评分 */
    val voteAverage: Double,

    /** 片单类型：STATUS_WANT_TO_WATCH 或 STATUS_WATCHED */
    val watchStatus: Int,

    /** 加入片单的时间戳（毫秒） */
    val addedAt: Long,
) {
    companion object {
        /** 想看 */
        const val STATUS_WANT_TO_WATCH = 0

        /** 已看 */
        const val STATUS_WATCHED = 1
    }
}
