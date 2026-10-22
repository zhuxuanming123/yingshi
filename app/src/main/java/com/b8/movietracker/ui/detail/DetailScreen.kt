package com.b8.movietracker.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.ui.components.PosterImage
import com.b8.movietracker.ui.common.EmptyView
import com.b8.movietracker.ui.common.ErrorView
import com.b8.movietracker.ui.common.LoadingView
import com.b8.movietracker.ui.common.UiState
import java.util.Locale

/**
 * 影视详情页：海报大图、简介、上映时间、评分、收藏按钮。
 *
 * @param onBack 返回上一页的回调（popBackStack）
 */
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 收藏态独立订阅（与详情数据分离，收藏切换时只刷新按钮，不重新请求详情）
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorView(state.message, viewModel::loadDetail)
        is UiState.Empty -> EmptyView("影片不存在")
        is UiState.Success -> DetailContent(
            movie = state.data,
            isFavorite = isFavorite,
            onBack = onBack,
            onToggleFavorite = viewModel::toggleFavorite,
        )
    }
}

/** 详情内容（仅在 Success 状态渲染） */
@Composable
private fun DetailContent(
    movie: Movie,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 顶部：海报大图 + 返回按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
        ) {
            PosterImage(
                posterUrl = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color(0xFF333333),
                )
            }
        }

        Column(Modifier.padding(16.dp)) {
            Text(text = movie.title, style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⭐ ${String.format(Locale.US, "%.1f", movie.voteAverage)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF57C00),
                    fontWeight = FontWeight.Bold,
                )
                if (movie.releaseDate != null) {
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "上映：${movie.releaseDate}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(text = "简介", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = movie.overview ?: "暂无简介",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onToggleFavorite,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isFavorite) "♥ 已收藏" else "♡ 收藏")
            }
        }
    }
}
