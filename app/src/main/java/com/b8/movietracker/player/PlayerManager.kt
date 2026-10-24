package com.b8.movietracker.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * 播放器管理器（任务6：播放器逻辑封装到独立工具类）。
 *
 * 职责：封装 ExoPlayer 的创建与释放，避免播放器实例散落在 UI/ViewModel 各处。
 *
 * 关于视频源：
 *   TMDB 的预告片是 YouTube 链接（youtube.com/watch?v=xxx），
 *   ExoPlayer 无法播放 YouTube 的加密流，故这里用 ExoPlayer 官方测试视频
 *   （Big Buck Bunny，公开直链）演示播放器完整能力。生产环境接入 YouTube 需用其官方 SDK。
 */
class PlayerManager(private val context: Context) {

    companion object {
        /** 演示视频源（ExoPlayer 官方测试视频，可直链播放） */
        const val DEMO_VIDEO_URL =
            "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    }

    /** 当前持有的播放器实例 */
    private var exoPlayer: ExoPlayer? = null

    /**
     * 创建并准备播放器（自动加载视频）。
     * 先释放旧实例，避免重复创建导致内存泄漏。
     */
    fun buildPlayer(url: String): ExoPlayer {
        release() // 释放旧实例
        return ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()          // 准备（异步加载）
            playWhenReady = true // 准备完成后自动播放
            exoPlayer = this
        }
    }

    /** 释放播放器（任务3：页面销毁时释放，避免内存泄漏） */
    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
