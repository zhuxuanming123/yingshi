package com.b8.movietracker.domain.repository

import com.b8.movietracker.domain.model.AppSettings
import com.b8.movietracker.domain.model.SortOption
import com.b8.movietracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * 配置项仓库（阶段：设置中心 + DataStore）。
 *
 * 约定：
 *   - 读：统一暴露 [settings] 这一个 Flow，界面与后台任务订阅同一数据源，
 *     任何一处写入都会实时推送给所有订阅者（无需手动刷新）；
 *   - 写：全部是 suspend 方法，由实现内部切到 IO 调度器执行，
 *     调用方（ViewModel / Worker）无需关心线程。
 */
interface SettingsRepository {

    /** 全部配置项的热数据流（首次发射即为当前配置） */
    val settings: Flow<AppSettings>

    /** 主题模式：跟随系统 / 浅色 / 深色 */
    suspend fun setThemeMode(mode: ThemeMode)

    /** 上映提醒总开关 */
    suspend fun setReminderEnabled(enabled: Boolean)

    /** 每日提醒时间 */
    suspend fun setReminderTime(hour: Int, minute: Int)

    /** 默认筛选偏好（首页打开时自动应用） */
    suspend fun setDefaultFilter(sortBy: SortOption, minVote: Double?, genreIds: List<Int>)
}