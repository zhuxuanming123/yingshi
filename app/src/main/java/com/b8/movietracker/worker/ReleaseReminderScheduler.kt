package com.b8.movietracker.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * WorkManager 调度器：负责开启/关闭上映提醒的周期任务。
 *
 * 时间策略（阶段：设置中心）：
 *   - 从"每 24 小时跑一次（时间随机）"升级为"每天在用户设定的时间跑一次"；
 *   - PeriodicWorkRequest 无法精确指定"每天 20:00"，所以用
 *     周期 24h + setInitialDelay(距下一次 20:00 的毫秒数) 来对齐；
 *   - 送达时间受系统省电策略影响，可能略有延迟（官方行为，已在设置页说明）。
 *
 * 更新策略：
 *   - 首次/无变化：KEEP（已有任务就保留，避免每次启动都重置计时）；
 *   - 用户在设置页改了时间：CANCEL_AND_REENQUEUE（旧的 initialDelay 必须作废）。
 */
object ReleaseReminderScheduler {

    private const val WORK_NAME = "release_reminder"

    /** 把"期望执行时间"写进 Worker 的输入数据，便于排查与将来做版本迁移 */
    const val KEY_REMINDER_HOUR = "reminder_hour"
    const val KEY_REMINDER_MINUTE = "reminder_minute"

    /**
     * 调度（或更新）每日提醒任务。
     *
     * @param hour 每日提醒时间（时）
     * @param minute 每日提醒时间（分）
     * @param restart true 表示配置发生变化，需要取消旧任务并按新时间重建
     */
    fun schedule(context: Context, hour: Int, minute: Int, restart: Boolean = false) {
        val request = PeriodicWorkRequestBuilder<ReleaseReminderWorker>(
            24, TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    KEY_REMINDER_HOUR to hour,
                    KEY_REMINDER_MINUTE to minute,
                ),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            if (restart) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP // 已存在则保留，不重复创建
            },
            request,
        )
    }

    /** 关闭提醒 */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * 距离"下一个 hour:minute"还有多少毫秒。
     *
     * 用 Calendar 而不是 java.time，是因为 minSdk 24，
     * java.time 需要 API 26+（或开启脱糖，本项目未开启）。
     * 抽成 internal 便于将来写单元测试。
     */
    internal fun initialDelayMillis(
        hour: Int,
        minute: Int,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val target = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // 今天的时间点已过 → 顺延到明天同一时刻
        if (!target.after(calendar)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - calendar.timeInMillis
    }
}