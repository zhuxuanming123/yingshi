package com.b8.movietracker.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager 调度器：负责开启/关闭上映提醒的周期任务。
 *
 * 关键点：
 *   - PeriodicWorkRequest 是周期任务，这里每 24 小时执行一次。
 *   - enqueueUniquePeriodicWork + KEEP 策略：避免重复开启时创建多个任务。
 *   - WorkManager 会在系统合适的时机执行（省电、Doze 模式、App 被杀后也能恢复）。
 */
object ReleaseReminderScheduler {

    private const val WORK_NAME = "release_reminder"

    /** 开启周期提醒（每 24 小时扫描一次片单） */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReleaseReminderWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // 已存在则保留，不重复创建
            request,
        )
    }

    /** 关闭提醒 */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
