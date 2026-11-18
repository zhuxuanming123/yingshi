package com.b8.movietracker.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.b8.movietracker.data.local.db.AppDatabase
import com.b8.movietracker.util.DateUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * ContentProvider 的 Hilt 入口点。
 *
 * ContentProvider 由系统实例化、不走 Hilt 的构造函数注入，
 * 但可以通过 EntryPointAccessors 从 SingletonComponent 里取到现成的单例，
 * 这样小组件读到的数据库和 App 内用的是同一个 Room 实例（共享连接池/缓存），
 * 而不是再开一份。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MovieProviderEntryPoint {
    fun database(): AppDatabase
}

/**
 * 只读片单 ContentProvider。
 *
 * 桌面小组件（独立 APK）通过它读取主 App 的 Room 片单：
 *
 *   content://com.b8.movietracker.provider/movies            → 全部片单（按加入时间倒序）
 *   content://com.b8.movietracker.provider/movies/upcoming   → 想看且未上映（按上映日期正序，最多 20 条）
 *
 * 权限：Manifest 里声明 android:readPermission = com.b8.movietracker.permission.READ_MOVIES，
 * 小组件 APK 在清单里 <uses-permission> 该权限后才能读到数据。
 *
 * 注意：这里直接用 Room 的 openHelper 执行 SQL（而不是 DAO 的 Flow），
 * 因为 ContentProvider 的 query 必须是同步返回 Cursor 的；Room 允许通过
 * SupportSQLiteDatabase.query() 走同一条数据库连接，不会额外开线程池。
 */
class MovieContentProvider : ContentProvider() {

    private val database: AppDatabase by lazy {
        val appContext = checkNotNull(context) { "ContentProvider 尚未 attach 到 Context" }
            .applicationContext
        EntryPointAccessors.fromApplication(appContext, MovieProviderEntryPoint::class.java)
            .database()
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        // 外部传入的 projection 走白名单过滤，过滤后为空则回退成 *（禁止拼进任意字符串）
        val columns = projection
            ?.filter { it in MovieProviderContract.ALLOWED_COLUMNS }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = ", ")
            ?: "*"

        // 说明：故意忽略外部的 selection / sortOrder / selectionArgs，
        // 只暴露两条固定语义的查询，避免外部拼条件越过「只读白名单」的限制。
        val path = uri.pathSegments.joinToString("/")
        val sql: String
        val bindArgs: Array<Any?>?
        when (path) {
            MovieProviderContract.PATH_MOVIES -> {
                sql = "SELECT $columns FROM movies ORDER BY addedAt DESC"
                bindArgs = null
            }

            MovieProviderContract.PATH_UPCOMING -> {
                sql = "SELECT $columns FROM movies " +
                    "WHERE watchStatus = 0 AND releaseDate IS NOT NULL AND releaseDate >= ? " +
                    "ORDER BY releaseDate ASC LIMIT ${MovieProviderContract.MAX_UPCOMING}"
                bindArgs = arrayOf<Any?>(DateUtils.today())
            }

            else -> throw IllegalArgumentException(
                "未知的查询路径：$uri（仅支持 /${MovieProviderContract.PATH_MOVIES} " +
                    "与 /${MovieProviderContract.PATH_UPCOMING}）"
            )
        }

        // SupportSQLiteDatabase 的两个重载：无参 SQL 与带绑定参数，按需选择
        val readableDatabase = database.openHelper.readableDatabase
        return if (bindArgs == null) {
            readableDatabase.query(sql)
        } else {
            readableDatabase.query(sql, bindArgs)
        }
    }

    override fun getType(uri: Uri): String = MovieProviderContract.MIME_TYPE

    /** 片单是只读接口：外部 App 不允许写入，写入请走主 App 的 Repository */
    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("片单为只读接口，不支持 insert")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("片单为只读接口，不支持 update")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("片单为只读接口，不支持 delete")
}