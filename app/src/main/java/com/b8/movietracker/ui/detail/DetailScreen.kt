package com.b8.movietracker.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.ui.common.EmptyView
import com.b8.movietracker.ui.common.ErrorView
import com.b8.movietracker.ui.common.LoadingView
import com.b8.movietracker.ui.common.UiState
import com.b8.movietracker.ui.components.PosterImage
import java.util.Locale

/**
 * 影视详情页：海报、简介、预告片、想看/已看 + 评分。
 */
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWantToWatch by viewModel.isWantToWatch.collectAsStateWithLifecycle()
    val isWatched by viewModel.isWatched.collectAsStateWithLifecycle()
    val trailerKey by viewModel.trailerKey.collectAsStateWithLifecycle()
    var showRatingDialog by remember { mutableStateOf(false) }

    when (val state = uiState) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorView(state.message, viewModel::loadDetail)
        is UiState.Empty -> EmptyView("影片不存在")
        is UiState.Success -> DetailContent(
            movie = state.data,
            isWantToWatch = isWantToWatch,
            isWatched = isWatched,
            trailerKey = trailerKey,
            onBack = onBack,
            onToggleWantToWatch = viewModel::toggleWantToWatch,
            onMarkWatched = { showRatingDialog = true },
        )
    }

    // 标记「已看」弹窗
    if (showRatingDialog) {
        RatingDialog(
            onConfirm = { rating, review ->
                viewModel.markWatched(rating, review)
                showRatingDialog = false
            },
            onDismiss = { showRatingDialog = false },
        )
    }
}

/** 详情内容 */
@Composable
private fun DetailContent(
    movie: Movie,
    isWantToWatch: Boolean,
    isWatched: Boolean,
    trailerKey: String?,
    onBack: () -> Unit,
    onToggleWantToWatch: () -> Unit,
    onMarkWatched: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
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

            Spacer(Modifier.height(16.dp))

            Text(text = "预告片", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (trailerKey != null) {
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/watch?v=$trailerKey"),
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("▶ 观看官方预告片")
                }
            } else {
                Text(
                    text = "暂无预告片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            }

            Spacer(Modifier.height(24.dp))

            // 想看 / 已看 两个按钮
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onToggleWantToWatch,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isWantToWatch) "✓ 想看" else "＋ 想看")
                }
                Button(
                    onClick = onMarkWatched,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isWatched) "✓ 已看" else "＋ 已看")
                }
            }
        }
    }
}
