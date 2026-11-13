package com.b8.movietracker.domain.model

import java.util.Locale

/**
 * 主题模式（阶段：设置中心）。
 *
 * 三个取值覆盖 Material 的推荐实践：
 *   - SYSTEM：跟随系统深浅色（默认，尊重用户系统设置）
 *   - LIGHT / DARK：强制浅色 / 深色
 */
enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色模式"),
    DARK("深色模式");

    companion object {
        /** 反序列化：DataStore 里读到非法/空值时回落到「跟随系统」 */
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * 全局配置项（全部持久化在 Jetpack DataStore 中）。
 *
 * 设计要点：
 *   - 这是纯 Kotlin 数据类，不含任何 Android 依赖，方便单测；
 *   - 字段全部给默认值，DataStore 首次读写（键不存在）时即为"出厂设置"；
 *   - [defaultFilter] 把"默认筛选偏好"转成首页可直接使用的 [FilterCriteria]。
 */
data class AppSettings(
    /** 主题模式 */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** 上映提醒总开关 */
    val reminderEnabled: Boolean = false,
    /** 每日提醒时间（时） */
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    /** 每日提醒时间（分） */
    val reminderMinute: Int = DEFAULT_REMINDER_MINUTE,
    /** 默认筛选：排序方式 */
    val defaultSortBy: SortOption = SortOption.POPULARITY_DESC,
    /** 默认筛选：最低评分（null = 不限） */
    val defaultMinVote: Double? = null,
    /** 默认筛选：影片类型 id 集合（空 = 不限） */
    val defaultGenreIds: List<Int> = emptyList(),
) {

    /** 默认筛选偏好 → 首页筛选条件（不含年份范围，年份属于临时筛选） */
    val defaultFilter: FilterCriteria
        get() = FilterCriteria(
            genreIds = defaultGenreIds,
            minVote = defaultMinVote,
            yearRange = null,
            sortBy = defaultSortBy,
        )

    /** 形如 "20:00" 的提醒时间文案 */
    val reminderTimeLabel: String
        get() = String.format(Locale.US, "%02d:%02d", reminderHour, reminderMinute)

    companion object {
        const val DEFAULT_REMINDER_HOUR = 20
        const val DEFAULT_REMINDER_MINUTE = 0

        /** 出厂默认配置（DataStore 无数据时使用） */
        val DEFAULT = AppSettings()
    }
}