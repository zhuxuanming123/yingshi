package com.b8.movietracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.b8.movietracker.domain.model.AppSettings
import com.b8.movietracker.domain.model.SortOption
import com.b8.movietracker.domain.model.ThemeMode
import com.b8.movietracker.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 Preferences DataStore 的配置仓库实现。
 *
 * 线程模型：
 *   - 读：`dataStore.data` 本身就在 IO 线程读取文件，映射成 [AppSettings] 后
 *     通过 Flow 下发，`distinctUntilChanged` 避免相同配置重复触发重组；
 *   - 写：统一走 [update] → `withContext(Dispatchers.IO) { dataStore.edit { } }`，
 *     既满足"所有 IO 都在协程里执行"，也保证调用方不会被磁盘 IO 卡住。
 *
 * 容错：
 *   读取时若磁盘文件损坏抛 IOException，降级为默认配置而不是让 App 崩溃。
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                // 文件损坏 / 读取失败：回落到空配置（即全部默认值），保证 App 可用
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> preferences.toAppSettings() }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        update { preferences -> preferences[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        update { preferences -> preferences[Keys.REMINDER_ENABLED] = enabled }
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        update { preferences ->
            // 越界值就地纠正，避免脏数据导致 WorkManager 计算初始延迟时异常
            preferences[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            preferences[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    override suspend fun setDefaultFilter(
        sortBy: SortOption,
        minVote: Double?,
        genreIds: List<Int>,
    ) {
        update { preferences ->
            preferences[Keys.DEFAULT_SORT] = sortBy.name
            // null 表示"不限"，此时移除键而不是写一个哨兵值
            if (minVote == null) {
                preferences.remove(Keys.DEFAULT_MIN_VOTE)
            } else {
                preferences[Keys.DEFAULT_MIN_VOTE] = minVote
            }
            preferences[Keys.DEFAULT_GENRES] = genreIds.map { it.toString() }.toSet()
        }
    }

    /** 所有写操作统一入口：IO 线程 + DataStore 事务提交 */
    private suspend fun update(block: (MutablePreferences) -> Unit) {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences -> block(preferences) }
        }
    }
}

/** Preferences → 领域模型（键缺失时取默认值，天然兼容老版本升级） */
private fun Preferences.toAppSettings(): AppSettings {
    val hour = this[Keys.REMINDER_HOUR] ?: AppSettings.DEFAULT_REMINDER_HOUR
    val minute = this[Keys.REMINDER_MINUTE] ?: AppSettings.DEFAULT_REMINDER_MINUTE
    val sortName = this[Keys.DEFAULT_SORT]
    return AppSettings(
        themeMode = ThemeMode.fromName(this[Keys.THEME_MODE]),
        reminderEnabled = this[Keys.REMINDER_ENABLED] ?: false,
        reminderHour = hour.coerceIn(0, 23),
        reminderMinute = minute.coerceIn(0, 59),
        defaultSortBy = SortOption.entries.firstOrNull { it.name == sortName }
            ?: SortOption.POPULARITY_DESC,
        defaultMinVote = this[Keys.DEFAULT_MIN_VOTE],
        defaultGenreIds = this[Keys.DEFAULT_GENRES]
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .sorted(),
    )
}

/** DataStore 的键名集中管理，避免字符串散落 */
private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    val DEFAULT_SORT = stringPreferencesKey("default_sort")
    val DEFAULT_MIN_VOTE = doublePreferencesKey("default_min_vote")
    val DEFAULT_GENRES = stringSetPreferencesKey("default_genres")
}