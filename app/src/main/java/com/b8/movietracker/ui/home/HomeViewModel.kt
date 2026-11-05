package com.b8.movietracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.FilterCriteria
import com.b8.movietracker.domain.model.Genre
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.recommend.RecommendEngine
import com.b8.movietracker.domain.repository.FavoriteRepository
import com.b8.movietracker.domain.repository.MovieRepository
import com.b8.movietracker.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 ViewModel：热门列表 + 类型字典 + 组合筛选 + 个性化推荐。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {

    /** 类型字典（筛选面板用） */
    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

    /** 当前生效的筛选条件 */
    private val _currentCriteria = MutableStateFlow(FilterCriteria())
    val currentCriteria: StateFlow<FilterCriteria> = _currentCriteria.asStateFlow()

    /** 影片列表状态 */
    private val _uiState = MutableStateFlow<UiState<List<Movie>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Movie>>> = _uiState.asStateFlow()

    /**
     * 个性化推荐列表（任务3/4）。
     * 用 combine 组合「用户行为」与「热门候选」，任一变化都自动重新计算推荐，
     * 实现"新增观影记录/评分/收藏后自动刷新"。
     */
    val recommendedMovies: StateFlow<List<Movie>> = combine(
        favoriteRepository.getAllMovies(),   // 用户行为数据（已看+想看）
        repository.getPopularMovies(),       // 候选影片库（热门）
    ) { userMovies, popularMovies ->
        RecommendEngine.recommend(userMovies, popularMovies)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        loadGenres()
        loadPopularMovies()
    }

    /** 加载类型字典（失败自动重试，解决网络偶发 SSL 握手失败） */
    fun loadGenres() {
        viewModelScope.launch {
            repository.getMovieGenres()
                .retry(2)
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

    /** 应用筛选 */
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
