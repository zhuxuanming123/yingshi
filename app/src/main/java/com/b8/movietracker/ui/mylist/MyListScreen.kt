package com.b8.movietracker.ui.mylist

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
 * 我的片单页：展示收藏（想看/已看）的影片列表。
 *
 * 数据是响应式的：在详情页点击收藏/取消收藏后，本页列表会自动刷新，
 * 无需手动刷新（由 repository 的 Flow 驱动）。
 */
@Composable
fun MyListScreen(
    onMovieClick: (Long) -> Unit,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "我的片单",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )

        UiStateView(
            state = uiState,
            onRetry = { /* 响应式数据源，无需手动重试；保留回调以统一接口 */ },
        ) { movies ->
            LazyColumn {
                items(movies, key = { it.id }) { movie ->
                    MovieListItem(movie = movie, onClick = { onMovieClick(movie.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}
