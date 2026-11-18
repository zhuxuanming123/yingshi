package com.b8.movietracker.widget.p1.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.b8.movietracker.widget.p1.data.WidgetRepository
import com.b8.movietracker.widget.p1.ui.WidgetDemoActivity

/**
 * 小组件里的点击跳转（PendingIntent）集中管理。
 *
 * 两个入口：
 *   1. 打开主 App —— 优先用 packageManager.getLaunchIntentForPackage(主 App)，
 *      并把影片 id 放进 EXTRA_MOVIE_ID（主 App 的 MainActivity 靠它直接跳详情页）；
 *      主 App 未安装时降级到本模块的演示页，保证点击有反馈。
 *   2. 手动刷新 —— 给中号组件的刷新按钮发一条显式广播，由 Provider 的 onReceive 处理。
 */
object WidgetIntents {

    /** 与主 App MainActivity.EXTRA_MOVIE_ID 保持一致（跨 APK 契约） */
    const val EXTRA_MOVIE_ID = "movie_id"

    /** 打开主 App；传入 movieId 时直接进详情页 */
    fun openMainApp(context: Context, movieId: Long? = null): PendingIntent {
        // requestCode 用 movieId 区分，避免不同影片复用同一个 PendingIntent
        val requestCode = movieId?.toInt() ?: REQUEST_CODE_OPEN_APP
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(WidgetRepository.MAIN_PACKAGE)
            ?: return openDemoActivity(context, requestCode)

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (movieId != null) launchIntent.putExtra(EXTRA_MOVIE_ID, movieId)
        return PendingIntent.getActivity(context, requestCode, launchIntent, pendingFlags())
    }

    /** 中号组件右上角「刷新」按钮 */
    fun refreshWidgets(context: Context): PendingIntent {
        val intent = Intent(context, MediumUpcomingWidgetProvider::class.java)
            .setAction(MediumUpcomingWidgetProvider.ACTION_REFRESH)
        return PendingIntent.getBroadcast(context, REQUEST_CODE_REFRESH, intent, pendingFlags())
    }

    /** 主 App 不存在时的兜底：打开本模块的演示页，至少能告诉用户发生了什么 */
    private fun openDemoActivity(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetDemoActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, requestCode, intent, pendingFlags())
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private const val REQUEST_CODE_OPEN_APP = 0
    private const val REQUEST_CODE_REFRESH = 1001
}