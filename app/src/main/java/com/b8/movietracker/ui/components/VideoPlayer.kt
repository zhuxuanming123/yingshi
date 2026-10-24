package com.b8.movietracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * 视频播放器 Compose 封装（任务3：生命周期管控 + 任务4：基础播放控制）。
 *
 * 用 AndroidView 包裹 Media3 的 PlayerView：
 *   - PlayerView 自带控制栏（播放/暂停、进度条拖动、全屏按钮），无需手写控件。
 *   - useController = true 显示控制栏。
 *
 * 生命周期管控（防内存泄漏/省电）：
 *   - 退到后台（ON_PAUSE）→ 暂停播放
 *   - 回到前台（ON_RESUME）→ 若之前在播则恢复
 *   通过 LifecycleEventObserver 监听生命周期，DisposableEffect 在组件销毁时移除监听。
 */
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // 监听生命周期：后台暂停、前台恢复
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (exoPlayer.playWhenReady) exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = exoPlayer
                useController = true // 显示控制栏（播放/暂停、进度条、全屏）
            }
        },
        modifier = modifier,
    )
}
