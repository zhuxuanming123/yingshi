package com.b8.movietracker.widget.p1.widget

import android.content.Context
import android.widget.RemoteViews
import com.b8.movietracker.widget.p1.data.WidgetData

/**
 * 小号组件（2×2）：片单中待映影片数量 + 一键打开 App。
 *
 * 只覆写「怎么渲染」，生命周期与线程模型都复用 [BaseUpcomingWidgetProvider]。
 */
class SmallUpcomingWidgetProvider : BaseUpcomingWidgetProvider() {

    override suspend fun buildViews(context: Context, data: WidgetData): RemoteViews =
        WidgetRenderer.small(context, data)
}