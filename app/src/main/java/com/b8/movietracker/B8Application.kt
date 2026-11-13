package com.b8.movietracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.b8.movietracker.domain.repository.SettingsRepository
import com.b8.movietracker.worker.ReleaseReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用入口 Application 类，四个职责：
 *   1. @HiltAndroidApp —— Hilt 依赖注入入口
 *   2. ImageLoaderFactory —— 配置全局 Coil 图片加载器（内存+磁盘缓存）
 *   3. Configuration.Provider —— 把 HiltWorkerFactory 交给 WorkManager，
 *      让 @HiltWorker 注解的 Worker 能被 Hilt 注入依赖（MovieDao / SettingsRepository）
 *   4. 配置订阅者 —— 监听 DataStore 里的「上映提醒」配置，
 *      设置一变就实时同步 WorkManager 周期任务（开启/关闭/改时间）
 */
@HiltAndroidApp
class B8Application : Application(), ImageLoaderFactory, Configuration.Provider {

    /** Hilt 提供的 Worker 工厂，用于创建 @HiltWorker 注解的 Worker */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** 配置项仓库（DataStore） */
    @Inject
    lateinit var settingsRepository: SettingsRepository

    /** 应用级协程作用域：随进程存活，不需要手动取消 */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** WorkManager 配置：使用 Hilt 的 WorkerFactory，否则 @HiltWorker 无法注入 */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        observeReminderSettings()
    }

    /**
     * 订阅提醒配置，把「DataStore 里的配置」映射为「WorkManager 里的任务」。
     *
     * 三种情况：
     *   - 开关打开：按设定时间调度（首次用 KEEP，避免每次启动都重置计时；配置变化才重建）
     *   - 开关关闭：取消任务（配置仍然保留，下次打开还能恢复）
     *   - 时间修改：restart=true → CANCEL_AND_REENQUEUE，让新的 initialDelay 生效
     */
    private fun observeReminderSettings() {
        applicationScope.launch {
            var firstEmission = true
            settingsRepository.settings
                .map { settings ->
                    ReminderConfig(
                        enabled = settings.reminderEnabled,
                        hour = settings.reminderHour,
                        minute = settings.reminderMinute,
                    )
                }
                .distinctUntilChanged()
                .collect { config ->
                    if (config.enabled) {
                        ReleaseReminderScheduler.schedule(
                            context = this@B8Application,
                            hour = config.hour,
                            minute = config.minute,
                            restart = !firstEmission,
                        )
                    } else {
                        ReleaseReminderScheduler.cancel(this@B8Application)
                    }
                    firstEmission = false
                }
        }
    }

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

    /** 提醒配置的"最小可比较单位"：只有开关或时间变化才会触发重新调度 */
    private data class ReminderConfig(
        val enabled: Boolean,
        val hour: Int,
        val minute: Int,
    )
}