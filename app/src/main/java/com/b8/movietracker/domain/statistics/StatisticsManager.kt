package com.b8.movietracker.domain.statistics

import com.b8.movietracker.domain.model.Movie
import java.util.Calendar

/**
 * 观影统计结果（任务4）。
 */
data class WatchStatistics(
    /** 已看影片总数 */
    val totalWatched: Int,

    /** 平均评分（有评分的影片的平均） */
    val averageRating: Double,

    /** 本月观影数量 */
    val watchedThisMonth: Int,

    /** 类型分布（genre id -> 数量） */
    val genreDistribution: Map<Int, Int>,
)

/**
 * 统计管理器（任务5：独立的自定义业务逻辑模块，与 UI 解耦）。
 *
 * 纯 Kotlin、无 Android/UI 依赖，输入"已看影片列表"输出统计结果，可单独单元测试。
 * 类型分布输出 genre id -> 数量，由 UI 层结合类型字典映射成中文名展示。
 */
object StatisticsManager {

    fun compute(watchedMovies: List<Movie>): WatchStatistics {
        val total = watchedMovies.size

        // 平均评分：只统计填了评分的影片
        val rated = watchedMovies.filter { it.userRating != null }
        val average = if (rated.isEmpty()) 0.0 else rated.map { it.userRating!! }.average()

        // 本月观影数
        val thisMonth = watchedMovies.count { isThisMonth(it.watchedDate) }

        // 类型分布：扁平化所有影片的类型，统计每个 genre id 出现次数
        val genreDistribution = watchedMovies
            .flatMap { it.genreIds }
            .groupingBy { it }
            .eachCount()

        return WatchStatistics(
            totalWatched = total,
            averageRating = average,
            watchedThisMonth = thisMonth,
            genreDistribution = genreDistribution,
        )
    }

    /** 判断时间戳是否在当前自然月内 */
    private fun isThisMonth(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
    }
}
