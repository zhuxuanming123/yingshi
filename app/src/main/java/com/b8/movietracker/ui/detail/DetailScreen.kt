package com.b8.movietracker.ui.detail

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.ui.common.EmptyView
import com.b8.movietracker.ui.common.ErrorView
import com.b8.movietracker.ui.common.LoadingView
import com.b8.movietracker.ui.common.UiState
import com.b8.movietracker.ui.components.PosterImage
import com.b8.movietracker.util.GallerySaver
import com.b8.movietracker.util.ShareCardStore
import java.util.Locale

/**
 * 影视详情页：海报、简介、预告片、想看/已看 + 评分 + 分享卡片。
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
    val shareCardState by viewModel.shareCardState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    var showRatingDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Android 9 及以下保存相册才需要存储权限；被拒绝时只提示，分享入口不受影响
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.saveShareCard() else viewModel.notifyStoragePermissionDenied()
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

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
            onShare = viewModel::generateShareCard,
        )
    }

    // 分享卡片浮层：生成中 / 预览导出 / 失败重试
    ShareCardOverlay(
        state = shareCardState,
        onDismiss = viewModel::dismissShareCard,
        onRetry = viewModel::generateShareCard,
        onShare = {
            val ready = shareCardState as? ShareCardState.Ready
            val movie = (uiState as? UiState.Success)?.data
            if (ready != null && movie != null) launchShareSheet(context, movie, ready)
        },
        onSave = {
            val needsPermission = GallerySaver.needsLegacyStoragePermission() &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                viewModel.saveShareCard()
            }
        },
    )

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

/**
 * 调起系统分享面板。
 *
 * 走 FileProvider 的 content:// URI + FLAG_GRANT_READ_URI_PERMISSION，
 * 目标 App 只拿到这一次性的读权限，既不用申请存储权限，也不会暴露整个缓存目录。
 */
private fun launchShareSheet(context: Context, movie: Movie, ready: ShareCardState.Ready) {
    val uri: Uri = ShareCardStore.uri(context, ready.file)
    val score = String.format(Locale.US, "%.1f", movie.voteAverage)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "《${movie.title}》⭐ $score —— 来自 B8 影视追踪")
        clipData = ClipData.newRawUri("share_card", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享影片卡片"))
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
    onShare: () -> Unit,
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
            // 分享入口：生成影片专属分享卡片（后半透明圆底保证在海报上也能看清）
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color(0x66000000), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "生成分享卡片",
                    tint = Color.White,
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

            Spacer(Modifier.height(12.dp))

            // 次要分享入口：按钮比图标更好发现，两者都进同一个生成流程
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("📤 生成分享卡片")
            }
        }
    }
}
