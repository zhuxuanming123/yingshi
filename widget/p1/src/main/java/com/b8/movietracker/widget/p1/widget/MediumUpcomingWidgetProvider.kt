package com.b8.movietracker.widget.p1.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.b8.movietracker.widget.p1.data.WidgetData

/**
 * 中号组件（4×3）：3 条即将上映（海报 + 标题 + 上映时间）。
 *
 * 除了系统触发的 onUpdate，还响应自带的「刷新」按钮广播
 * [ACTION_REFRESH]（按钮的 PendingIntent 由 WidgetIntents 创建）。
 */
class MediumUpcomingWidgetProvider : BaseUpcomingWidgetProvider() {

    override suspend fun buildViews(context: Context, data: WidgetData): RemoteViews =
        WidgetRenderer.medium(context, data)

    override fun onReceive(context: Context, intent: Intent) {
        // 先交给父类处理系统标准 action（APPWIDGET_UPDATE / ENABLED / DELETED …）
        super.onReceive(context, intent)

        if (intent.action == ACTION_REFRESH) {
            Log.d(TAG, "收到手动刷新广播")
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, MediumUpcomingWidgetProvider::class.java)
            )
            refresh(context, manager, ids)
        }
    }

    companion object {
        /** 与 AndroidManifest 中 <action> 保持一致 */
        const val ACTION_REFRESH = "com.b8.movietracker.widget.p1.action.REFRESH"

        private const val TAG = "B8Widget"
    }
}