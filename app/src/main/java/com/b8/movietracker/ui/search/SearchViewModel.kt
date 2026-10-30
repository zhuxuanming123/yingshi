package com.b8.movietracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.MovieRepository
import com.b8.movietracker.domain.repository.SearchHistoryRepository
import com.b8.movietracker.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索 ViewModel：关键词搜索 + 搜索历史（任务3）。
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** 搜索结果状态 */
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

    /** 最近搜索历史（关键词列表，响应式） */
    val searchHistory: StateFlow<List<String>> = searchHistoryRepository.getSearchHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** 供输入框 onValueChange 调用 */
    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    /** 清空搜索历史 */
    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearSearchHistory()
        }
    }

    /** 一次搜索：记录历史（非空关键词）+ 请求结果 */
    private fun searchFlow(keyword: String): Flow<UiState<List<Movie>>> = flow {
        emit(UiState.Loading)
        val trimmed = keyword.trim()
        if (trimmed.isNotEmpty()) {
            searchHistoryRepository.addSearchHistory(trimmed) // 任务3：记录搜索历史
        }
        val result = repository.searchMovies(keyword).first()
        emit(if (result.isEmpty()) UiState.Empty else UiState.Success(result))
    }.catch { error ->
        emit(UiState.Error(error.message ?: "搜索失败"))
    }
}
