package com.b8.movietracker.domain.filter

import com.b8.movietracker.domain.model.FilterCriteria

/**
 * 筛选引擎（任务2）：负责筛选条件的【组装】【校验】【生效】。
 *
 * 为什么独立成模块？
 *   - 与 UI 完全解耦：UI 只负责收集用户选择、构造 FilterCriteria，
 *     不关心 TMDB 参数怎么拼；本模块只关心"条件 -> 查询参数"的转换。
 *   - 纯 Kotlin、无 Android 依赖，可单独单元测试。
 *
 * 职责：
 *   1. buildQuery：把 FilterCriteria 转成 TMDB discover 接口的查询参数
 *   2. validate：校验条件合法性（评分范围、年份范围）
 */
object FilterEngine {

    /**
     * 组装筛选条件为 TMDB discover 查询参数。
     */
    fun buildQuery(criteria: FilterCriteria): Map<String, String> {
        val query = mutableMapOf<String, String>()

        // 类型：多选，用逗号拼接（TMDB with_genres 参数格式）
        if (criteria.genreIds.isNotEmpty()) {
            query["with_genres"] = criteria.genreIds.joinToString(",")
        }

        // 评分下限（如 7.0 = 7 分以上）
        criteria.minVote?.let { query["vote_average.gte"] = it.toString() }

        // 上映年份范围（TMDB 用 primary_release_date 的起止日期表达）
        criteria.yearRange?.let {
            query["primary_release_date.gte"] = "${it.first}-01-01"
            query["primary_release_date.lte"] = "${it.last}-12-31"
        }

        // 排序方式
        query["sort_by"] = criteria.sortBy.tmdbValue

        return query
    }

    /**
     * 校验筛选条件是否合法。
     * @return true = 合法可执行，false = 条件非法（UI 应提示用户）
     */
    fun validate(criteria: FilterCriteria): Boolean {
        // 评分必须在 0~10 之间
        if (criteria.minVote != null && criteria.minVote !in 0.0..10.0) return false
        // 年份范围起始不能大于结束
        if (criteria.yearRange != null && criteria.yearRange.first > criteria.yearRange.last) {
            return false
        }
        return true
    }
}
