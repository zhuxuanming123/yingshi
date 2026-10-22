package com.b8.movietracker

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口 Application 类。
 *
 * 两个职责：
 *   1. @HiltAndroidApp —— Hilt 依赖注入入口
 *   2. 实现 ImageLoaderFactory —— 配置全局 Coil 图片加载器（任务1：内存+磁盘缓存）
 *
 * 为什么实现 ImageLoaderFactory？
 *   Coil 会自动发现并调用 newImageLoader()，把返回的 ImageLoader 作为全局单例，
 *   之后所有 AsyncImage 都自动使用这份配置（内存/磁盘缓存、淡入动画）。
 */
@HiltAndroidApp
class B8Application : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        // 内存缓存：占应用可用内存的 25%（防止 OOM，任务3）
        val memoryCache = MemoryCache.Builder(this)
            .maxSizePercent(0.25)
            .build()

        // 磁盘缓存：200MB，存到 cacheDir/image_cache（任务1）
        val diskCache = DiskCache.Builder()
            .directory(cacheDir.resolve("image_cache"))
            .maxSizeBytes(200L * 1024 * 1024)
            .build()

        return ImageLoader.Builder(this)
            .memoryCache(memoryCache)
            .diskCache(diskCache)
            .crossfade(true)  // 加载完成淡入，避免图片突然出现/闪烁
            .build()
    }
}
