package com.b8.movietracker.widget.p1.widget

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import com.b8.movietracker.widget.p1.R
import com.b8.movietracker.widget.p1.data.Source
import com.b8.movietracker.widget.p1.data.UpcomingMovie
import com.b8.movietracker.widget.p1.data.WidgetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 把 [WidgetData] 渲染成 RemoteViews。
 *
 * 注意：桌面进程不认识本模块的 View 类，RemoteViews 只支持系统白名单控件
 * （LinearLayout / TextView / ImageView 等），所以两款组件都用原生 XML 布局，
 * 而不是 Compose。
 */
object WidgetRenderer {

    /** 海报解码的目标像素：30dp × 40dp 的槽位，按 1.6 倍密度留余量 */
    private const val POSTER_WIDTH_PX = 112
    private const val POSTER_HEIGHT_PX = 144

    /** 中号最多展示 3 条（任务要求） */
    private const val MAX_ROWS = 3

    // ------------------------------------------------------------------
    // 小号：待映数量 + 一键打开 App
    // ------------------------------------------------------------------
    fun small(context: Context, data: WidgetData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_small)

        if (data.hasData) {
            views.setTextViewText(R.id.widget_small_count, data.upcomingCount.toString())
            views.setTextViewText(R.id.widget_small_label, context.getString(R.string.widget_small_label))
            views.setTextViewText(R.id.widget_small_hint, context.getString(R.string.widget_small_hint))
        } else {
            // 空态：数字给 0，提示改成「去添加」，不留一片空白
            views.setTextViewText(R.id.widget_small_count, "0")
            views.setTextViewText(R.id.widget_small_label, context.getString(R.string.widget_small_label))
            views.setTextViewText(R.id.widget_small_hint, context.getString(R.string.widget_small_no_data_hint))
        }

        // 整块点击 = 打开主 App
        views.setOnClickPendingIntent(R.id.widget_small_root, WidgetIntents.openMainApp(context))
        return views
    }

    // ------------------------------------------------------------------
    // 中号：3 条即将上映（海报 + 标题 + 上映时间）
    // ------------------------------------------------------------------
    suspend fun medium(context: Context, data: WidgetData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_medium)
        val rows = data.movies.take(MAX_ROWS)

        views.setTextViewText(
            R.id.widget_medium_count,
            if (rows.isEmpty()) "" else context.getString(R.string.widget_medium_count, data.upcomingCount),
        )
        views.removeAllViews(R.id.widget_medium_list)

        if (rows.isEmpty()) {
            views.setViewVisibility(R.id.widget_medium_list, View.GONE)
            views.setViewVisibility(R.id.widget_medium_empty, View.VISIBLE)
            applyEmptyState(context, views, data.source)
        } else {
            views.setViewVisibility(R.id.widget_medium_list, View.VISIBLE)
            views.setViewVisibility(R.id.widget_medium_empty, View.GONE)
            // 三张海报并行下载：串行最坏要 3 × 4s，会超过广播 10s 的存活窗口
            val posters = coroutineScope {
                rows.map { movie ->
                    async(Dispatchers.IO) {
                        PosterLoader.load(context, movie.posterUrl, POSTER_WIDTH_PX, POSTER_HEIGHT_PX)
                    }
                }.map { it.await() }
            }
            rows.forEachIndexed { index, movie ->
                views.addView(R.id.widget_medium_list, buildRow(context, movie, posters[index]))
            }
        }

        // 标题栏空白处 + 刷新按钮的点击
        views.setOnClickPendingIntent(R.id.widget_medium_root, WidgetIntents.openMainApp(context))
        views.setOnClickPendingIntent(R.id.widget_medium_refresh, WidgetIntents.refreshWidgets(context))
        views.setOnClickPendingIntent(R.id.widget_medium_footer, WidgetIntents.openMainApp(context))
        return views
    }

    /** 空态三种文案：没装主 App / 没权限 / 有 App 但片单里没有待映影片 */
    private fun applyEmptyState(context: Context, views: RemoteViews, source: Source) {
        val (titleRes, hintRes) = when (source) {
            Source.UNAVAILABLE -> R.string.widget_no_app_title to R.string.widget_no_app_hint
            Source.DENIED -> R.string.widget_denied_title to R.string.widget_denied_hint
            else -> R.string.widget_empty_title to R.string.widget_empty_hint
        }
        views.setTextViewText(R.id.widget_medium_empty_title, context.getString(titleRes))
        views.setTextViewText(R.id.widget_medium_empty_hint, context.getString(hintRes))
    }

    /** 构造一条影片条目（海报已在外面并行加载好） */
    private fun buildRow(context: Context, movie: UpcomingMovie, poster: Bitmap?): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_medium_item)
        row.setTextViewText(R.id.widget_item_title, movie.title.ifBlank { context.getString(R.string.widget_untitled) })
        row.setTextViewText(R.id.widget_item_date, formatReleaseDate(context, movie.releaseDate))

        if (poster != null) {
            row.setImageViewBitmap(R.id.widget_item_poster, poster)
        } else {
            row.setImageViewResource(R.id.widget_item_poster, R.drawable.widget_poster_placeholder)
        }

        // 点击某条 → 主 App 详情页（movieId 放进 EXTRA_MOVIE_ID）
        row.setOnClickPendingIntent(R.id.widget_item_root, WidgetIntents.openMainApp(context, movie.id))
        return row
    }

    /**
     * TMDB 的 releaseDate 是 "yyyy-MM-dd"，这里转成「12月18日 上映」。
     * 解析失败就原样展示，保证不会因为脏数据崩溃。
     */
    private fun formatReleaseDate(context: Context, releaseDate: String?): String {
        if (releaseDate.isNullOrBlank()) return context.getString(R.string.widget_date_unknown)
        val label = runCatching {
            val parts = releaseDate.split("-")
            "${parts[1].toInt()}月${parts[2].toInt()}日"
        }.getOrNull() ?: releaseDate
        return context.getString(R.string.widget_date_pattern, label)
    }
}