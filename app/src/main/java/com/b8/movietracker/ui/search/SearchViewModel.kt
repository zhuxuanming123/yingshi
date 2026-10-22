package com.b8.movietracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.MovieRepository
import com.b8.movietracker.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 搜索 ViewModel：监听搜索框输入，实时返回过滤结果。
 *
 * 用到的高级 Flow 操作符（答辩亮点）：
 *   - debounce(300)：输入停顿 300ms 才触发搜索，避免每个字符都发请求
 *   - distinctUntilChanged：关键词没变时不重复搜索
 *   - flatMapLatest：新关键词到来时，取消上一次未完成的搜索（只保留最新结果）
 *   - stateIn：把冷流转成热流 StateFlow，共享给 UI
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MovieRepository,
) : ViewModel() {

    /** 当前搜索关键词（用户输入） */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** 搜索结果状态：由关键词流经一系列操作符驱动 */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<List<Movie>>> = _query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { keyword -> searchFlow(keyword) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    /** 供输入框 onValueChange 调用 */
    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    /** 把一次关键词搜索包装成带「加载/成功/错误/空」状态的 Flow */
    private fun searchFlow(keyword: String): kotlinx.coroutines.flow.Flow<UiState<List<Movie>>> = flow {
        emit(UiState.Loading)
        val result = repository.searchMovies(keyword).first()
        emit(if (result.isEmpty()) UiState.Empty else UiState.Success(result))
    }.catch { error ->
        emit(UiState.Error(error.message ?: "搜索失败"))
    }
}
