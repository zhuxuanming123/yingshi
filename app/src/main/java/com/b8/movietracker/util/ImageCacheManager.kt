package com.b8.movietracker.util

import android.content.Context
import coil.imageLoader
import java.util.Locale

/**
 * 图片缓存工具（设置中心 - 缓存查看与清理）。
 *
 * 缓存对象是 B8Application.newImageLoader() 里配置的全局 Coil ImageLoader：
 *   - 内存缓存：可用内存的 25%
 *   - 磁盘缓存：cacheDir/image_cache，上限 200MB
 *
 * 注意：Coil 的磁盘缓存读写是阻塞 IO，
 * 调用方（SettingsViewModel）必须切到 IO 协程后再调用本类。
 */
object ImageCacheManager {

    /** 当前磁盘缓存占用的字节数（读取失败按 0 处理，不影响设置页展示） */
    fun sizeBytes(context: Context): Long =
        runCatching { context.imageLoader.diskCache?.size ?: 0L }.getOrDefault(0L)

    /** 一键清理：先清内存缓存，再清磁盘缓存 */
    fun clear(context: Context) {
        val imageLoader = context.imageLoader
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    /** 把字节数格式化成人类可读的大小，如 "12.3 MB" */
    fun formatSize(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}