package com.b8.movietracker.widget.p1.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 小组件海报加载器。
 *
 * 为什么不能用 Coil 的 AsyncImage：
 *   桌面小组件跑在「桌面进程」里，只能通过 RemoteViews 传 Bitmap，
 *   不能用 Compose/View 树，也不能在桌面上直接发网络请求。
 *   所以这里在小组件自己的进程里下载 → 解码 → setImageViewBitmap。
 *
 * 缓存策略（三级，避免频繁耗流量）：
 *   1. 内存 LruCache：同一次刷新里 3 张图只解一次；
 *   2. 磁盘 cacheDir/widget_posters/：跨刷新复用（系统清理缓存时会回收）；
 *   3. 网络：HttpURLConnection，4 秒超时，失败就返回 null 让 UI 显示占位图。
 */
object PosterLoader {

    private const val TAG = "PosterLoader"
    private const val TIMEOUT_MS = 4_000
    private const val CACHE_DIR = "widget_posters"

    private val memoryCache = LruCache<String, Bitmap>(8)

    /**
     * 加载海报并解码到接近需求尺寸（避免把 342px 宽的大图塞进 30dp 的槽位）。
     *
     * @return 成功返回 Bitmap，任何失败返回 null（调用方回退到占位图）
     */
    suspend fun load(
        context: Context,
        url: String?,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? {
        if (url.isNullOrBlank()) return null
        memoryCache.get(url)?.let { return it }

        return withContext(Dispatchers.IO) {
            val file = cacheFile(context, url)
            val bytes = if (file.exists()) {
                runCatching { file.readBytes() }.getOrNull()
            } else {
                download(url)?.also { data ->
                    // 写缓存失败不影响本次展示
                    runCatching { file.writeBytes(data) }
                }
            }

            val bitmap = bytes?.let { decode(it, reqWidth, reqHeight) }
            if (bitmap != null) memoryCache.put(url, bitmap)
            bitmap
        }
    }

    private fun cacheFile(context: Context, url: String): File {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        // 用 hashCode 做文件名：URL 里可能有 / 和 ?，不能直接当文件名
        return File(dir, "${url.hashCode()}.img")
    }

    private fun download(url: String): ByteArray? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }.onFailure {
        Log.w(TAG, "海报下载失败：$url", it)
    }.getOrNull()

    private fun decode(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? = runCatching {
        // 先只读尺寸，算出合适的采样率，避免 OOM
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }.getOrNull()

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        var sampleSize = 1
        if (reqWidth <= 0 || reqHeight <= 0) return sampleSize
        while (width / (sampleSize * 2) >= reqWidth && height / (sampleSize * 2) >= reqHeight) {
            sampleSize *= 2
        }
        return sampleSize
    }
}