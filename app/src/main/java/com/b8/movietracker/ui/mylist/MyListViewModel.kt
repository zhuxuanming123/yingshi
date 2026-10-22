package com.b8.movietracker.ui.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.FavoriteRepository
import com.b8.movietracker.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 我的片单 ViewModel：展示收藏的影片列表（依赖本地收藏数据源）。
 *
 * 数据响应式：详情页切换收藏后，本页自动刷新，无需手动操作。
 */
@HiltViewModel
class MyListViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {

    val uiState: StateFlow<UiState<List<Movie>>> = favoriteRepository.getFavoriteMovies()
        .map { movies ->
            if (movies.isEmpty()) UiState.Empty else UiState.Success(movies)
        }
        .catch { error ->
            emit(UiState.Error(error.message ?: "加载失败"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )
}
