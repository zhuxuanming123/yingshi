package com.b8.movietracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.ui.common.UiStateView
import com.b8.movietracker.ui.components.MovieListItem
import com.b8.movietracker.ui.components.PosterImage

/**
 * 首页：为你推荐 + 热门影视列表 + 筛选入口。
 */
@Composable
fun HomeScreen(
    onMovieClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val currentCriteria by viewModel.currentCriteria.collectAsStateWithLifecycle()
    val recommendedMovies by viewModel.recommendedMovies.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }

    LaunchedEffect(showFilter) {
        if (showFilter && genres.isEmpty()) {
            viewModel.loadGenres()
        }
    }

    Column(Modifier.fillMaxSize()) {
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
            OutlinedButton(onClick = { showFilter = true }) {
                Text(if (currentCriteria.isEmpty) "筛选" else "筛选 ✓")
            }
        }

        UiStateView(
            state = uiState,
            onRetry = {
                if (currentCriteria.isEmpty) {
                    viewModel.loadPopularMovies()
                } else {
                    viewModel.applyFilter(currentCriteria)
                }
            },
        ) { movies ->
            LazyColumn {
                // 为你推荐板块（任务3：横向滑动 Top10）
                if (recommendedMovies.isNotEmpty()) {
                    item {
                        RecommendSection(
                            movies = recommendedMovies,
                            onMovieClick = onMovieClick,
                        )
                    }
                }
                // 热门列表
                items(movies, key = { it.id }) { movie ->
                    MovieListItem(movie = movie, onClick = { onMovieClick(movie.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (showFilter) {
        FilterSheet(
            genres = genres,
            currentCriteria = currentCriteria,
            onApply = viewModel::applyFilter,
            onDismiss = { showFilter = false },
        )
    }
}

/** 为你推荐横向滑动板块 */
@Composable
private fun RecommendSection(
    movies: List<Movie>,
    onMovieClick: (Long) -> Unit,
) {
    Column {
        Text(
            text = "为你推荐",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(movies, key = { it.id }) { movie ->
                RecommendCard(movie = movie, onClick = { onMovieClick(movie.id) })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** 推荐卡片（小海报 + 标题） */
@Composable
private fun RecommendCard(movie: Movie, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
    ) {
        PosterImage(
            posterUrl = movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier
                .size(width = 100.dp, height = 150.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = movie.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
