package com.b8.movietracker.ui.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.FavoriteRepository
import com.b8.movietracker.ui.common.UiState
import com.b8.movietracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 我的片单统计信息（任务5：自定义业务逻辑，非纯增删改查）。
 * @param total 片单内影片总数
 * @param upcoming 即将上映（上映日期在未来）的影片数量
 */
data class MyListStats(
    val total: Int,
    val upcoming: Int,
)

/**
 * 我的片单 ViewModel：展示收藏列表 + 统计片单信息。
 */
@HiltViewModel
class MyListViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
) : ViewModel() {

    /** 收藏列表状态 */
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

    /**
     * 统计信息（任务5）：片单总数 + 即将上映数量。
     * 响应式：收藏列表变化时自动重新统计。
     */
    val stats: StateFlow<MyListStats> = favoriteRepository.getFavoriteMovies()
        .map { movies ->
            MyListStats(
                total = movies.size,
                upcoming = movies.count { DateUtils.isUpcoming(it.releaseDate) },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyListStats(total = 0, upcoming = 0),
        )
}
