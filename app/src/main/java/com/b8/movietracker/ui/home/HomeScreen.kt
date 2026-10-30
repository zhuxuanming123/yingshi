package com.b8.movietracker.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.ui.common.UiStateView
import com.b8.movietracker.ui.components.MovieListItem

/**
 * 首页：热门影视列表 + 筛选入口。
 */
@Composable
fun HomeScreen(
    onMovieClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val currentCriteria by viewModel.currentCriteria.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }

    // 打开筛选面板时，如果类型还没加载出来（之前网络偶发失败），重新加载
    LaunchedEffect(showFilter) {
        if (showFilter && genres.isEmpty()) {
            viewModel.loadGenres()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 标题行 + 筛选入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "热门影视",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            // 筛选按钮（已筛选时显示标识）
            OutlinedButton(onClick = { showFilter = true }) {
                Text(if (currentCriteria.isEmpty) "筛选" else "筛选 ✓")
            }
        }

        UiStateView(
            state = uiState,
            onRetry = {
                // 重试：有筛选则重新筛选，否则重新加载热门
                if (currentCriteria.isEmpty) {
                    viewModel.loadPopularMovies()
                } else {
                    viewModel.applyFilter(currentCriteria)
                }
            },
        ) { movies ->
            LazyColumn {
                items(movies, key = { it.id }) { movie ->
                    MovieListItem(movie = movie, onClick = { onMovieClick(movie.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    // 筛选面板
    if (showFilter) {
        FilterSheet(
            genres = genres,
            currentCriteria = currentCriteria,
            onApply = viewModel::applyFilter,
            onDismiss = { showFilter = false },
        )
    }
}
