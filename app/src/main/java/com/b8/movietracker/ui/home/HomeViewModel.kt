package com.b8.movietracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.MovieRepository
import com.b8.movietracker.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 ViewModel：加载热门影片列表。
 *
 * 关键点：
 *   - viewModelScope：协程作用域绑定 ViewModel 生命周期，
 *     当 ViewModel 被清理时协程自动取消，避免内存泄漏与内存浪费。
 *   - StateFlow：对外暴露不可变状态，UI 通过 collectAsStateWithLifecycle 订阅。
 *   - catch：统一捕获异常，转为 Error 状态，UI 永不因异常崩溃。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository,
) : ViewModel() {

    /** 对外暴露的只读状态 */
    private val _uiState = MutableStateFlow<UiState<List<Movie>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Movie>>> = _uiState.asStateFlow()

    init {
        loadPopularMovies()
    }

    /** 加载热门影片（供 init 与错误态重试共用） */
    fun loadPopularMovies() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getPopularMovies()
                .catch { error ->
                    // 任何异常都转为 Error 状态，不让异常冒泡到 UI 层
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
                .collect { movies ->
                    _uiState.value = if (movies.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(movies)
                    }
                }
        }
    }
}
