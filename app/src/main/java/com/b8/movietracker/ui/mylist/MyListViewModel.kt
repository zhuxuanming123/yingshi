package com.b8.movietracker.ui.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.Genre
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.FavoriteRepository
import com.b8.movietracker.domain.repository.MovieRepository
import com.b8.movietracker.domain.statistics.StatisticsManager
import com.b8.movietracker.domain.statistics.WatchStatistics
import com.b8.movietracker.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 我的片单 ViewModel：想看/已看 列表 + 观影统计（任务3/4）。
 */
@HiltViewModel
class MyListViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val movieRepository: MovieRepository,
) : ViewModel() {

    /** 「想看」列表 */
    val wantToWatch: StateFlow<UiState<List<Movie>>> = favoriteRepository
        .getMoviesByStatus(FavoriteRepository.STATUS_WANT_TO_WATCH)
        .map { movies -> if (movies.isEmpty()) UiState.Empty else UiState.Success(movies) }
        .catch { error -> emit(UiState.Error(error.message ?: "加载失败")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** 「已看」列表 */
    val watched: StateFlow<UiState<List<Movie>>> = favoriteRepository
        .getMoviesByStatus(FavoriteRepository.STATUS_WATCHED)
        .map { movies -> if (movies.isEmpty()) UiState.Empty else UiState.Success(movies) }
        .catch { error -> emit(UiState.Error(error.message ?: "加载失败")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** 观影统计（任务4/5：由 StatisticsManager 计算） */
    val statistics: StateFlow<WatchStatistics> = favoriteRepository
        .getMoviesByStatus(FavoriteRepository.STATUS_WATCHED)
        .map { movies -> StatisticsManager.compute(movies) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WatchStatistics(0, 0.0, 0, emptyMap()),
        )

    /** 类型字典（用于把类型分布的 id 映射成中文名） */
    val genres: StateFlow<List<Genre>> = movieRepository.getMovieGenres()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
