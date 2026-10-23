package com.b8.movietracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 应用入口 Application 类，三个职责：
 *   1. @HiltAndroidApp —— Hilt 依赖注入入口
 *   2. ImageLoaderFactory —— 配置全局 Coil 图片加载器（内存+磁盘缓存）
 *   3. Configuration.Provider —— 把 HiltWorkerFactory 交给 WorkManager，
 *      让 @HiltWorker 注解的 Worker 能被 Hilt 注入依赖（如 MovieDao）
 */
@HiltAndroidApp
class B8Application : Application(), ImageLoaderFactory, Configuration.Provider {

    /** Hilt 提供的 Worker 工厂，用于创建 @HiltWorker 注解的 Worker */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** WorkManager 配置：使用 Hilt 的 WorkerFactory，否则 @HiltWorker 无法注入 */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader {
        // 内存缓存：占应用可用内存的 25%（防止 OOM）
        val memoryCache = MemoryCache.Builder(this)
            .maxSizePercent(0.25)
            .build()
        // 磁盘缓存：200MB
        val diskCache = DiskCache.Builder()
            .directory(cacheDir.resolve("image_cache"))
            .maxSizeBytes(200L * 1024 * 1024)
            .build()
        return ImageLoader.Builder(this)
            .memoryCache(memoryCache)
            .diskCache(diskCache)
            .crossfade(true)
            .build()
    }
}
