package com.b8.movietracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.FavoriteRepository
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
 * 详情 ViewModel：加载详情（网络）+ 维护收藏态（本地）。
 *
 * 为什么拆成两个数据源？
 *   - 详情数据来自网络（MovieRepository）
 *   - 收藏态来自本地（FavoriteRepository）
 *   二者独立变化，用两个 StateFlow 分别暴露，UI 各自订阅。
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val favoriteRepository: FavoriteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** 从路由参数读取影片 ID */
    private val movieId: Long = savedStateHandle.get<Long>("movieId") ?: -1L

    /** 详情数据状态（网络） */
    private val _uiState = MutableStateFlow<UiState<Movie>>(UiState.Loading)
    val uiState: StateFlow<UiState<Movie>> = _uiState.asStateFlow()

    /** 是否已收藏（本地，响应式） */
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    init {
        loadDetail()
        observeFavorite()
    }

    /** 加载详情（供 init 与错误态重试共用） */
    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            movieRepository.getMovieDetail(movieId)
                .catch { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
                .collect { movie ->
                    _uiState.value = UiState.Success(movie)
                }
        }
    }

    /** 观察收藏列表，实时更新「是否已收藏」 */
    private fun observeFavorite() {
        viewModelScope.launch {
            favoriteRepository.getFavoriteMovies().collect { favorites ->
                _isFavorite.value = favorites.any { it.id == movieId }
            }
        }
    }

    /** 切换收藏：拿到当前影片对象后交给收藏仓库处理 */
    fun toggleFavorite() {
        val movie = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(movie)
        }
    }
}
