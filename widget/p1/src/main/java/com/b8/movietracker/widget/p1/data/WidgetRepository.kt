package com.b8.movietracker.widget.p1.data

import android.content.Context
import android.net.Uri
import android.util.Log

/**
 * 主 App 片单的「跨进程只读客户端」。
 *
 * 数据链路：
 *   小组件 APK ──ContentResolver──▶ content://com.b8.movietracker.provider/movies/upcoming
 *                                        │
 *                                  主 App MovieContentProvider
 *                                        │
 *                                   Room(b8_movie.db) 的 movies 表
 *
 * 为什么要走 ContentProvider 而不是直接读 db 文件：
 *   /data/data/com.b8.movietracker/databases/b8_movie.db 是主 App 私有目录，
 *   别的 APK 没有读权限；ContentProvider 是 Android 官方提供的跨进程数据共享通道，
 *   配合自定义权限可以做到「只读 + 可控」。
 *
 * 注意：这里的 authority / 路径 / 列名是跨 APK 契约，必须与主 App 的
 * MovieProviderContract 保持一致。
 */
object WidgetRepository {

    private const val TAG = "WidgetRepository"

    /** 主 App 包名 */
    const val MAIN_PACKAGE = "com.b8.movietracker"

    /** 主 App 暴露的 Provider authority */
    const val AUTHORITY = "com.b8.movietracker.provider"

    /** 读取片单所需的自定义权限 */
    const val READ_PERMISSION = "com.b8.movietracker.permission.READ_MOVIES"

    private val UPCOMING_URI: Uri = Uri.parse("content://$AUTHORITY/movies/upcoming")

    /** 只查组件需要的列（provider 侧还有一层白名单过滤） */
    private val PROJECTION = arrayOf("id", "title", "posterUrl", "releaseDate")

    /**
     * 读取待映片单。
     *
     * 这个方法会被 AppWidgetProvider 在后台线程调用，
     * 因此内部直接做同步查询；任何异常都降级成空数据，绝不让组件崩掉。
     */
    fun load(context: Context): WidgetData {
        return try {
            val cursor = context.contentResolver
                .query(UPCOMING_URI, PROJECTION, null, null, null)
                ?: return WidgetData(0, emptyList(), Source.UNAVAILABLE)

            cursor.use { c ->
                val idIndex = c.getColumnIndexOrThrow("id")
                val titleIndex = c.getColumnIndexOrThrow("title")
                val posterIndex = c.getColumnIndexOrThrow("posterUrl")
                val dateIndex = c.getColumnIndexOrThrow("releaseDate")

                val movies = ArrayList<UpcomingMovie>(c.count)
                while (c.moveToNext()) {
                    movies += UpcomingMovie(
                        id = c.getLong(idIndex),
                        title = c.getString(titleIndex).orEmpty(),
                        posterUrl = if (c.isNull(posterIndex)) null else c.getString(posterIndex),
                        releaseDate = if (c.isNull(dateIndex)) null else c.getString(dateIndex),
                    )
                }
                WidgetData(movies.size, movies, Source.MAIN_APP)
            }
        } catch (e: SecurityException) {
            // 典型场景：先装小组件、后装主 App，权限在安装小组件时还未存在
            Log.w(TAG, "读取片单被拒绝（请先安装主 App，再重装小组件以授予权限）", e)
            WidgetData(0, emptyList(), Source.DENIED)
        } catch (e: Exception) {
            // 主 App 未安装时通常返回 null（上面已处理）；这里兜住 Provider 崩溃等异常
            Log.w(TAG, "读取主 App 片单失败", e)
            WidgetData(0, emptyList(), Source.UNAVAILABLE)
        }
    }
}