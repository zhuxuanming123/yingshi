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
 * 详情 ViewModel：详情 + 收藏状态（想看/已看）+ 预告片 + 观影记录。
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

    /** 是否在「想看」列表 */
    private val _isWantToWatch = MutableStateFlow(false)
    val isWantToWatch: StateFlow<Boolean> = _isWantToWatch.asStateFlow()

    /** 是否在「已看」列表 */
    private val _isWatched = MutableStateFlow(false)
    val isWatched: StateFlow<Boolean> = _isWatched.asStateFlow()

    /** 预告片 YouTube key */
    private val _trailerKey = MutableStateFlow<String?>(null)
    val trailerKey: StateFlow<String?> = _trailerKey.asStateFlow()

    init {
        loadDetail()
        observeWatchStatus()
        loadTrailer()
    }

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

    /** 观察「想看」「已看」两个列表，更新按钮状态 */
    private fun observeWatchStatus() {
        viewModelScope.launch {
            favoriteRepository.getMoviesByStatus(FavoriteRepository.STATUS_WANT_TO_WATCH)
                .collect { list -> _isWantToWatch.value = list.any { it.id == movieId } }
        }
        viewModelScope.launch {
            favoriteRepository.getMoviesByStatus(FavoriteRepository.STATUS_WATCHED)
                .collect { list -> _isWatched.value = list.any { it.id == movieId } }
        }
    }

    private fun loadTrailer() {
        viewModelScope.launch {
            movieRepository.getMovieTrailer(movieId).collect { key ->
                _trailerKey.value = key
            }
        }
    }

    /** 切换「想看」 */
    fun toggleWantToWatch() {
        val movie = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(movie)
        }
    }

    /** 标记「已看」：写入评分 + 短评 + 观影日期 */
    fun markWatched(rating: Double?, review: String?) {
        val movie = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            favoriteRepository.markWatched(
                movie.copy(
                    userRating = rating,
                    watchedDate = System.currentTimeMillis(),
                    review = review,
                )
            )
        }
    }
}
