package com.b8.movietracker.provider

import android.net.Uri

/**
 * 主 App 对外暴露的「片单读取契约」。
 *
 * 背景：桌面小组件是独立的 APK（:widget:p1），无法直接打开主 App 的 Room 数据库
 * （数据库文件在 /data/data/com.b8.movietracker/databases/ 下，属于应用私有目录）。
 * 因此主 App 用 ContentProvider 以「只读 + 自定义权限」的方式把片单暴露出去：
 *
 *   content://com.b8.movietracker.provider/movies           全部片单
 *   content://com.b8.movietracker.provider/movies/upcoming  待映片单（想看 + 上映日期 >= 今天）
 *
 * 安全措施：
 *   1. android:readPermission 限定为自定义 normal 权限，只有声明了该权限的 App 能读；
 *   2. 只实现 query，insert/update/delete 一律拒绝（外部无法篡改片单）；
 *   3. query 的 projection 做白名单过滤，避免拼 SQL 时被注入。
 *
 * 注意：小组件模块里有一份同样的常量（WidgetRepository），
 * 两边的 authority / 路径 / 列名必须保持一致，改这里要同步改那边。
 */
object MovieProviderContract {

    /** Provider 的 authority，需与 AndroidManifest 中的 android:authorities 一致 */
    const val AUTHORITY = "com.b8.movietracker.provider"

    /** 读取片单所需的自定义权限（在主 App 的清单里用 <permission> 声明） */
    const val READ_PERMISSION = "com.b8.movietracker.permission.READ_MOVIES"

    /** 全部片单 */
    const val PATH_MOVIES = "movies"

    /** 待映片单：watchStatus = 想看(0) 且 releaseDate >= 今天 */
    const val PATH_UPCOMING = "movies/upcoming"

    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_MOVIES")
    val UPCOMING_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_UPCOMING")

    // ---------------- 列名 ----------------
    const val COLUMN_ID = "id"
    const val COLUMN_TITLE = "title"
    const val COLUMN_POSTER_URL = "posterUrl"
    const val COLUMN_RELEASE_DATE = "releaseDate"
    const val COLUMN_VOTE_AVERAGE = "voteAverage"
    const val COLUMN_WATCH_STATUS = "watchStatus"
    const val COLUMN_ADDED_AT = "addedAt"

    /** 返回给外部的最小 MIME 类型（目录型 Cursor） */
    const val MIME_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.movie"

    /** 待映列表最多返回的条数（小组件只用前 3 条，留一点余量给计数） */
    const val MAX_UPCOMING = 20

    /**
     * projection 白名单：只允许下面这些列出现在 SQL 里。
     * （外部 App 传入的 projection 会被过滤，非法列直接丢弃后回退成 *）
     */
    val ALLOWED_COLUMNS: Set<String> = setOf(
        COLUMN_ID,
        COLUMN_TITLE,
        COLUMN_POSTER_URL,
        COLUMN_RELEASE_DATE,
        COLUMN_VOTE_AVERAGE,
        COLUMN_WATCH_STATUS,
        COLUMN_ADDED_AT,
    )
}