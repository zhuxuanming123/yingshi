package com.b8.movietracker.domain.model

/**
 * 筛选条件（任务1：多维度组合筛选）。
 *
 * 字段说明：
 *   - genreIds   影片类型（多选，空 = 不限）
 *   - minVote    最低评分（如 7.0 = 7 分以上，null = 不限）
 *   - yearRange  上映年份范围（如 2010..2020，null = 不限）
 *   - sortBy     排序方式
 */
data class FilterCriteria(
    val genreIds: List<Int> = emptyList(),
    val minVote: Double? = null,
    val yearRange: IntRange? = null,
    val sortBy: SortOption = SortOption.POPULARITY_DESC,
) {
    /** 是否没有任何筛选条件（等于"默认全部"） */
    val isEmpty: Boolean
        get() = genreIds.isEmpty() && minVote == null && yearRange == null &&
            sortBy == SortOption.POPULARITY_DESC
}

/**
 * 排序方式（任务1）。
 * @param label 中文展示名
 * @param tmdbValue TMDB discover 接口的 sort_by 参数值
 */
enum class SortOption(val label: String, val tmdbValue: String) {
    VOTE_DESC("评分降序", "vote_average.desc"),
    RELEASE_DESC("最新上映", "primary_release_date.desc"),
    POPULARITY_DESC("热门程度", "popularity.desc"),
}
