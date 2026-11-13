package com.b8.movietracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.b8.movietracker.data.local.MovieEntityMapper
import com.b8.movietracker.data.local.dao.MovieDao
import com.b8.movietracker.domain.repository.SettingsRepository
import com.b8.movietracker.notification.NotificationHelper
import com.b8.movietracker.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * 上映提醒后台任务（WorkManager 定时扫描本地片单）。
 *
 * 工作流程：
 *   0. 先读 DataStore 里的「上映提醒」总开关，关闭则直接结束（不发通知）；
 *   1. 从 Room 读取本地片单（收藏的影片）；
 *   2. 筛选上映日期在 7 天内的影片；
 *   3. 对每部影片发送系统通知。
 *
 * 关于线程：
 *   CoroutineWorker 继承自协程 Worker，doWork() 是 suspend，
 *   WorkManager 内部在后台线程池执行，绝不阻塞主线程。
 *
 * 关于注入：
 *   @HiltWorker + @AssistedInject 让 Hilt 自动注入 MovieDao / SettingsRepository，
 *   context/params 用 @Assisted 标记（由 WorkManager 提供，不由 Hilt 提供）。
 */
@HiltWorker
class ReleaseReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val movieDao: MovieDao,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // 0. 总开关：关闭状态下不做任何事（任务本身仍在，随时可被重新开启）
            val settings = settingsRepository.settings.first()
            if (!settings.reminderEnabled) {
                return Result.success()
            }

            // 1. 扫描本地片单（first() 取一次 Room 数据）
            val entities = movieDao.getAllMovies().first()

            // 2. 筛选 7 天内上映的影片
            val upcoming = entities.filter { entity ->
                DateUtils.isReleasingSoon(entity.releaseDate, withinDays = 7)
            }

            // 3. 逐个发送通知
            upcoming.forEach { entity ->
                val movie = MovieEntityMapper.toMovie(entity)
                NotificationHelper.showReleaseReminder(applicationContext, movie)
            }

            Result.success()
        } catch (e: Exception) {
            // 任何异常都不让 App 崩溃，返回 failure 让 WorkManager 稍后重试
            Result.failure()
        }
    }
}