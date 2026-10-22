package com.b8.movietracker.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.ui.common.UiStateView
import com.b8.movietracker.ui.components.MovieListItem

/**
 * 首页：热门影视列表。
 *
 * @param onMovieClick 列表项点击回调，用于导航到详情页（参数为影片 ID）
 * @param viewModel 由 Hilt 注入的 HomeViewModel（hiltViewModel() 自动获取）
 */
@Composable
fun HomeScreen(
    onMovieClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle：生命周期感知地订阅 StateFlow，
    // 界面不可见时停止收集，可见时恢复，避免后台无谓刷新（防泄漏/省资源）
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "热门影视",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )

        // 用统一状态视图渲染：加载/空/错误/成功 四种情况
        UiStateView(
            state = uiState,
            onRetry = viewModel::loadPopularMovies,
        ) { movies ->
            LazyColumn {
                // key = { it.id }：给每项稳定的 key，提升列表复用与动画正确性
                items(movies, key = { it.id }) { movie ->
                    MovieListItem(movie = movie, onClick = { onMovieClick(movie.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}
