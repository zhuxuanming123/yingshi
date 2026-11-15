package com.b8.movietracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.FilterCriteria
import com.b8.movietracker.domain.model.Genre
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.recommend.RecommendEngine
import com.b8.movietracker.domain.repository.FavoriteRepository
import com.b8.movietracker.domain.repository.MovieRepository
import com.b8.movietracker.domain.repository.SettingsRepository
import com.b8.movietracker.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** 类型字典（筛选面板用） */
    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

    /** 当前生效的筛选条件 */
    private val _currentCriteria = MutableStateFlow(FilterCriteria())
    val currentCriteria: StateFlow<FilterCriteria> = _currentCriteria.asStateFlow()

    /** 影片列表状态（首屏 / 刷新后的第一页） */
    private val _uiState = MutableStateFlow<UiState<List<Movie>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Movie>>> = _uiState.asStateFlow()

    /** 下拉刷新中（刷新期间保留旧内容，不闪白） */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** 正在加载下一页 */
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    /** 是否还有下一页（TMDB 每页 20 条，返回不足一页即视为到底） */
    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    /** 当前已加载到第几页 */
    private var currentPage = 1

    /** 首屏加载任务，切换筛选/刷新时取消旧任务 */
    private var loadJob: Job? = null

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
    }
        // 候选流是网络请求：断网时必须兜底，否则异常会冲出 stateIn 把 ViewModel 协程打挂
        // （表现为应用启动即闪退）。推荐只是首页的次要板块，失败就展示空列表。
        .catch { emit(emptyList()) }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        loadGenres()
        loadInitialMovies()
    }

    /**
     * 首次加载：先读 DataStore 里的「默认筛选偏好」。
     * 有默认偏好就直接按它筛选，没有才加载热门列表。
     */
    private fun loadInitialMovies() {
        viewModelScope.launch {
            val defaultFilter = settingsRepository.settings.first().defaultFilter
            if (defaultFilter.isEmpty) {
                loadPopularMovies()
            } else {
                applyFilter(defaultFilter)
            }
        }
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

    /** 加载热门影片（回到第一页） */
    fun loadPopularMovies() {
        _currentCriteria.value = FilterCriteria()
        loadFirstPage()
    }

    /** 应用筛选（回到第一页） */
    fun applyFilter(criteria: FilterCriteria) {
        _currentCriteria.value = criteria
        loadFirstPage()
    }

    /** 下拉刷新：重拉第一页，刷新期间保留旧列表（不闪白） */
    fun refresh() {
        if (_isRefreshing.value) return
        loadFirstPage(keepContentWhileLoading = true)
    }

    /**
     * 滑到列表底部时调用：加载下一页并追加到已有列表。
     * 失败时静默保留已加载内容，用户再次滑到底部会重试。
     */
    fun loadMore() {
        val current = _uiState.value as? UiState.Success ?: return
        if (_isLoadingMore.value || _isRefreshing.value || !_hasMore.value) return
        val criteria = _currentCriteria.value
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val nextPage = currentPage + 1
                val movies = fetchPage(nextPage, criteria)
                // 加载期间用户切换了筛选条件，丢弃这次结果
                if (criteria != _currentCriteria.value) return@launch
                currentPage = nextPage
                _hasMore.value = movies.size >= PAGE_SIZE
                val merged = (current.data + movies).distinctBy { it.id }
                _uiState.value = UiState.Success(merged)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 加载更多失败：保留已有内容，下次滑动重试
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * 加载第一页。
     * @param keepContentWhileLoading 下拉刷新时为 true：保留旧内容，只转刷新指示器。
     */
    private fun loadFirstPage(keepContentWhileLoading: Boolean = false) {
        loadJob?.cancel()
        val criteria = _currentCriteria.value
        loadJob = viewModelScope.launch {
            if (!keepContentWhileLoading) _uiState.value = UiState.Loading
            _isRefreshing.value = keepContentWhileLoading
            try {
                val movies = fetchPage(1, criteria)
                currentPage = 1
                _hasMore.value = movies.size >= PAGE_SIZE
                _uiState.value =
                    if (movies.isEmpty()) UiState.Empty else UiState.Success(movies)
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                // 已有内容时不把刷新失败变成整页错误，避免列表被清空
                if (_uiState.value !is UiState.Success) {
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** 按当前筛选条件拉取指定页；无筛选条件时走热门榜 */
    private suspend fun fetchPage(page: Int, criteria: FilterCriteria): List<Movie> =
        if (criteria.isEmpty) {
            repository.getPopularMovies(page).first()
        } else {
            repository.discoverMovies(criteria, page).first()
        }

    companion object {
        /** TMDB 单页返回条数，用于判断是否已到最后一页 */
        private const val PAGE_SIZE = 20
    }
}
