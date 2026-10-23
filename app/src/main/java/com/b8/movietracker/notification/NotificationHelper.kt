package com.b8.movietracker.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.b8.movietracker.MainActivity
import com.b8.movietracker.R
import com.b8.movietracker.domain.model.Movie

/**
 * 通知工具类（任务3：通知栏展示影片信息 + 点击打开详情页）。
 *
 * 职责：
 *   1. 创建通知渠道（Android 8+ 必须）
 *   2. 发送"影片即将上映"通知
 *   3. 通知点击 → PendingIntent 携带 movieId 跳转 MainActivity → 导航到详情页
 */
object NotificationHelper {

    const val CHANNEL_ID = "release_reminder"

    /** 创建通知渠道（Android 8.0+ 必须先创建，否则通知不显示） */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "上映提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "片单内影片上映日期临近时提醒"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /** 是否已授予通知权限（Android 13+ 运行时权限） */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * 发送"影片即将上映"通知。
     * 任务4：无通知权限时静默返回（不发、不崩溃），由 UI 层做降级提示。
     */
    fun showReleaseReminder(context: Context, movie: Movie) {
        // 无权限直接返回，不抛异常
        if (!hasPermission(context)) return
        createChannel(context)

        // 点击通知 → 打开 MainActivity，携带 movieId，导航到详情页
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_MOVIE_ID, movie.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            movie.id.toInt(), // 不同影片用不同 requestCode，避免通知互相覆盖
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("《${movie.title}》即将上映")
            .setContentText("上映日期：${movie.releaseDate ?: "即将公布"}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // 点击后自动消失
            .build()

        NotificationManagerCompat.from(context)
            .notify(movie.id.toInt(), notification)
    }
}
