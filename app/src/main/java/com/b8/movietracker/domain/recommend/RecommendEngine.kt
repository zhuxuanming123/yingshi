package com.b8.movietracker.domain.recommend

import com.b8.movietracker.domain.model.Movie

/**
 * 推荐引擎（标签加权推荐算法，任务1/2：核心自定义业务逻辑）。
 *
 * ── 算法原理（三步）─────────────────────────────────────────
 * 1. 提取标签：收集用户「已看 + 想看（收藏）」影片的 genreIds 作为兴趣标签。
 * 2. 计算权重：每个标签的权重 = Σ(该标签下每部用户影片的「贡献值」)。
 * 3. 匹配推荐：候选影片得分 = Σ(其所有标签的权重)，按得分降序取 TopN。
 *
 * ── 贡献值（权重计算规则）────────────────────────────────────
 * 某用户影片对某标签的贡献值 = 状态权重 + 评分权重 + 时间权重：
 *   - 状态权重：已看 = 2.0，想看 = 1.0（看过的比想看的更能代表兴趣）
 *   - 评分权重：userRating / 10，范围 0~1（评分越高越喜欢，贡献越大）
 *   - 时间权重：30 天内看过 +0.5（近期行为更有参考价值，时间衰减）
 *
 * ── 降级策略（任务5）────────────────────────────────────────
 * 用户行为数据不足（已看+想看 < MIN_BEHAVIOR_COUNT 部）时，
 * 无法可靠计算兴趣标签，直接返回候选库（热门榜单）作为推荐。
 *
 * 说明：本类是纯 Kotlin 的 object 单例，无任何 Android/网络/数据库依赖，
 *       与 UI、数据层完全解耦，可单独单元测试。
 */
object RecommendEngine {

    /** 行为数据不足阈值：少于该数量则降级展示热门 */
    private const val MIN_BEHAVIOR_COUNT = 3

    /** 时间权重：多少天内的观影算"近期" */
    private const val RECENT_DAYS = 30

    /**
     * 生成推荐列表（任务2 核心入口）。
     *
     * @param userMovies 用户行为数据（已看/想看，来自本地 Room）
     * @param candidateMovies 候选影片库（热门影片，来自 TMDB）
     * @param topN 推荐数量，默认 10
     * @return 排序后的推荐列表；行为数据不足时返回候选库前 topN（降级）
     */
    fun recommend(
        userMovies: List<Movie>,
        candidateMovies: List<Movie>,
        topN: Int = 10,
    ): List<Movie> {
        // 降级：行为数据不足，无法计算兴趣标签，直接返回热门
        if (userMovies.size < MIN_BEHAVIOR_COUNT) {
            return candidateMovies.take(topN)
        }

        val genreWeights = computeGenreWeights(userMovies)
        val watchedIds = userMovies.map { it.id }.toSet()

        return candidateMovies
            .filter { it.id !in watchedIds }          // 排除已看/收藏的，避免重复推荐
            .map { movie -> movie to scoreMovie(movie, genreWeights) }
            .sortedByDescending { it.second }         // 得分降序
            .take(topN)
            .map { it.first }
    }

    /**
     * 计算标签权重（任务2：按评分/时间/收藏状态加权）。
     * @return genreId -> 权重值 的映射
     */
    fun computeGenreWeights(userMovies: List<Movie>): Map<Int, Double> {
        val weights = mutableMapOf<Int, Double>()
        for (movie in userMovies) {
            val contribution = contributionOf(movie)
            for (genreId in movie.genreIds) {
                weights[genreId] = (weights[genreId] ?: 0.0) + contribution
            }
        }
        return weights
    }

    /** 计算某用户影片对标签的贡献值 */
    private fun contributionOf(movie: Movie): Double {
        // 状态权重：已看 2.0，想看 1.0
        val statusWeight = if (movie.watchStatus == 1) 2.0 else 1.0
        // 评分权重：0~1
        val ratingWeight = (movie.userRating ?: 0.0) / 10.0
        // 时间权重：近期观影 +0.5
        val timeWeight = if (isRecent(movie.watchedDate)) 0.5 else 0.0
        return statusWeight + ratingWeight + timeWeight
    }

    /** 候选影片得分 = 其所有标签权重之和 */
    private fun scoreMovie(movie: Movie, genreWeights: Map<Int, Double>): Double =
        movie.genreIds.sumOf { genreWeights[it] ?: 0.0 }

    /** 判断时间戳是否在最近 N 天内 */
    private fun isRecent(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val diff = System.currentTimeMillis() - timestamp
        return diff in 0..(RECENT_DAYS * 24L * 3600 * 1000)
    }
}
