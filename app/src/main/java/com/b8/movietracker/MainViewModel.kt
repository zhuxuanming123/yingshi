package com.b8.movietracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.AppSettings
import com.b8.movietracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 主 Activity 的 ViewModel：只做一件事 —— 在首帧渲染前把配置读出来。
 *
 * 为什么单独建 ViewModel：
 *   1. 主题模式要在 `setContent` 之前就有值，否则会先按浅色画一帧再变深色（闪烁）；
 *   2. 用 `SharingStarted.Eagerly` 让 DataStore 立即开始读取，配合启动画面
 *      `setKeepOnScreenCondition { !isSettingsLoaded }`，做到"配置就绪才淡出"；
 *   3. 之后继续以 StateFlow 形式供 Compose 订阅，用户在设置页切换主题时，
 *      这里会收到新值并触发重组（不重建 Activity，无闪烁）。
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** null 表示尚未从 DataStore 读到（首帧前的准备阶段） */
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /** 配置是否已就绪：启动画面用它决定何时结束 */
    val isSettingsLoaded: Boolean
        get() = settings.value != null
}