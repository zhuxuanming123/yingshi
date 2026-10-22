package com.b8.movietracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/** 海报占位背景色（加载中/失败/无地址时统一显示） */
private val PlaceholderColor = Color(0xFFE0E0E0)

/**
 * 可复用的海报图片组件（任务2：图片加载容错）。
 *
 * 统一处理三种情况，保证不空白、不崩溃：
 *   1. posterUrl 为空/空串 → 显示 🎬 占位
 *   2. 加载中            → placeholder 灰块
 *   3. 加载失败          → error 灰块（Coil 自动捕获，不会抛异常崩溃）
 *
 * 任务3 列表优化：组件内部用 Coil 的 AsyncImage，配合全局 ImageLoader 的
 *   内存缓存限制 + LazyColumn 的 item key，滑动时自动复用/取消加载，避免错位与 OOM。
 */
@Composable
fun PosterImage(
    posterUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(
        modifier = modifier.background(PlaceholderColor),
        contentAlignment = Alignment.Center,
    ) {
        if (posterUrl.isNullOrBlank()) {
            // 无图片地址：直接显示占位，不发起网络请求
            Text(text = "🎬", style = MaterialTheme.typography.titleLarge)
        } else {
            AsyncImage(
                model = posterUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                placeholder = ColorPainter(PlaceholderColor), // 加载中占位
                error = ColorPainter(PlaceholderColor),      // 加载失败占位
            )
        }
    }
}
