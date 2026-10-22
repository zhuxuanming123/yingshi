package com.b8.movietracker.domain.model

/**
 * 影片领域模型（任务4：数据模型）。
 *
 * 与 DTO 的区别：
 *   - 领域模型面向「业务」，字段已加工成 UI 可直接使用（如完整图片 URL）
 *   - DTO 面向「网络」，字段与 JSON 一一对应（如相对路径 poster_path）
 *
 * 字段说明（对应任务要求）：
 *   id         —— 影片 ID
 *   title      —— 标题
 *   posterUrl  —— 海报完整地址（已拼接 image base url，可直接给 Coil 加载）
 *   overview   —— 简介
 *   releaseDate—— 上映日期
 *   voteAverage—— 评分
 *   isFavorite —— 收藏状态（阶段6 与本地「想看/已看」清单打通）
 */
data class Movie(
    val id: Long,
    val title: String,
    val posterUrl: String?,
    val overview: String?,
    val releaseDate: String?,
    val voteAverage: Double,
    val isFavorite: Boolean = false,
)
