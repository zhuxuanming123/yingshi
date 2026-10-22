package com.b8.movietracker.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
 * 搜索页：顶部搜索框 + 搜索结果列表。
 */
@Composable
fun SearchScreen(
    onMovieClick: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        // 搜索输入框
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("搜索影片标题，如：星际") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
        )

        UiStateView(
            state = uiState,
            onRetry = { viewModel.onQueryChange(query) },
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
