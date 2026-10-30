package com.b8.movietracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.FilterCriteria
import com.b8.movietracker.domain.model.Genre
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.MovieRepository
import com.b8.movietracker.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 ViewModel：热门列表 + 类型字典 + 组合筛选。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository,
) : ViewModel() {

    /** 类型字典（任务1：从 TMDB 获取，供筛选面板展示） */
    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

    /** 当前生效的筛选条件 */
    private val _currentCriteria = MutableStateFlow(FilterCriteria())
    val currentCriteria: StateFlow<FilterCriteria> = _currentCriteria.asStateFlow()

    /** 影片列表状态 */
    private val _uiState = MutableStateFlow<UiState<List<Movie>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Movie>>> = _uiState.asStateFlow()

    init {
        loadGenres()
        loadPopularMovies()
    }

    /** 加载类型字典（失败自动重试，解决网络偶发 SSL 握手失败） */
    fun loadGenres() {
        viewModelScope.launch {
            repository.getMovieGenres()
                .retry(2) // 失败自动重试 2 次
                .catch { /* 多次失败后静默，筛选面板类型为空 */ }
                .collect { genres -> _genres.value = genres }
        }
    }

    /** 加载热门影片 */
    fun loadPopularMovies() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getPopularMovies()
                .catch { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
                .collect { movies ->
                    _uiState.value =
                        if (movies.isEmpty()) UiState.Empty else UiState.Success(movies)
                }
        }
    }

    /**
     * 应用筛选（任务1）。
     * 无筛选条件时回到热门列表；否则调用 discover 接口按条件筛选。
     */
    fun applyFilter(criteria: FilterCriteria) {
        _currentCriteria.value = criteria
        if (criteria.isEmpty) {
            loadPopularMovies()
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.discoverMovies(criteria)
                .catch { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
                .collect { movies ->
                    _uiState.value =
                        if (movies.isEmpty()) UiState.Empty else UiState.Success(movies)
                }
        }
    }
}
