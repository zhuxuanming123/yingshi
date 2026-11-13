package com.b8.movietracker.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.AppSettings
import com.b8.movietracker.domain.model.Genre
import com.b8.movietracker.domain.model.SortOption
import com.b8.movietracker.domain.model.ThemeMode
import com.b8.movietracker.domain.repository.MovieRepository
import com.b8.movietracker.domain.repository.SettingsRepository
import com.b8.movietracker.util.ImageCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 设置中心 ViewModel。
 *
 * 所有写操作都是「向 DataStore 写一条配置」，不做本地缓存：
 * 写完后 DataStore 的 Flow 会把新值推回来，界面自然刷新；
 * 同时 B8Application 订阅了同一份 Flow，会实时同步 WorkManager 提醒任务。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    movieRepository: MovieRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** 当前配置（首次订阅前给默认值，避免界面为空） */
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings.DEFAULT,
        )

    /** 类型字典（默认筛选偏好里选类型用） */
    val genres: StateFlow<List<Genre>> = movieRepository.getMovieGenres()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 图片磁盘缓存占用（字节） */
    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    // ===================== 外观 =====================

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    // ===================== 上映提醒 =====================

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReminderEnabled(enabled) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.setReminderTime(hour, minute) }
    }

    // ===================== 默认筛选偏好 =====================

    fun setDefaultSort(sortBy: SortOption) {
        val current = settings.value
        persistDefaultFilter(sortBy, current.defaultMinVote, current.defaultGenreIds)
    }

    fun setDefaultMinVote(minVote: Double?) {
        val current = settings.value
        persistDefaultFilter(current.defaultSortBy, minVote, current.defaultGenreIds)
    }

    fun toggleDefaultGenre(genreId: Int) {
        val current = settings.value
        val next = if (genreId in current.defaultGenreIds) {
            current.defaultGenreIds - genreId
        } else {
            current.defaultGenreIds + genreId
        }
        persistDefaultFilter(current.defaultSortBy, current.defaultMinVote, next)
    }

    private fun persistDefaultFilter(
        sortBy: SortOption,
        minVote: Double?,
        genreIds: List<Int>,
    ) {
        viewModelScope.launch {
            settingsRepository.setDefaultFilter(sortBy, minVote, genreIds)
        }
    }

    // ===================== 图片缓存 =====================

    /** 读取当前磁盘缓存大小（IO 线程） */
    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheSizeBytes.value = withContext(Dispatchers.IO) {
                ImageCacheManager.sizeBytes(context)
            }
        }
    }

    /** 一键清理缓存，清理后重新统计大小 */
    fun clearImageCache() {
        viewModelScope.launch {
            val afterClear = withContext(Dispatchers.IO) {
                ImageCacheManager.clear(context)
                ImageCacheManager.sizeBytes(context)
            }
            _cacheSizeBytes.value = afterClear
        }
    }
}