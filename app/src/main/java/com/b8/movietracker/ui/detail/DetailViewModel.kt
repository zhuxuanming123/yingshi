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
 * 详情 ViewModel：加载详情 + 维护收藏态 + 加载预告片信息。
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val favoriteRepository: FavoriteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: Long = savedStateHandle.get<Long>("movieId") ?: -1L

    /** 详情数据状态 */
    private val _uiState = MutableStateFlow<UiState<Movie>>(UiState.Loading)
    val uiState: StateFlow<UiState<Movie>> = _uiState.asStateFlow()

    /** 是否已收藏 */
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    /** 预告片的 YouTube key（无预告片为 null），UI 据此跳转 YouTube 观看 */
    private val _trailerKey = MutableStateFlow<String?>(null)
    val trailerKey: StateFlow<String?> = _trailerKey.asStateFlow()

    init {
        loadDetail()
        observeFavorite()
        loadTrailer()
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

    /** 加载预告片 YouTube key（任务1：对接 TMDB 视频接口） */
    private fun loadTrailer() {
        viewModelScope.launch {
            movieRepository.getMovieTrailer(movieId).collect { key ->
                _trailerKey.value = key
            }
        }
    }

    /** 切换收藏 */
    fun toggleFavorite() {
        val movie = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(movie)
        }
    }
}
