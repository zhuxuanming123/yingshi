package com.b8.movietracker.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 把分享卡片存进系统相册。
 *
 * 存储适配：
 * - Android 10+（API 29）：走 MediaStore + 分区存储，写入自己创建的
 *   `Pictures/B8MovieTracker/` 条目，**不需要任何权限**；
 * - Android 9 及以下：MediaStore 写入公共目录需要 WRITE_EXTERNAL_STORAGE，
 *   Manifest 里已用 `maxSdkVersion="28"` 限定，运行时由界面层按需申请；
 * - 权限被拒绝时这里会返回 false，调用方只提示，不影响系统分享。
 */
object GallerySaver {

    private const val ALBUM_NAME = "B8MovieTracker"

    /**
     * 保存位图到相册。
     *
     * @return true 表示写入成功；false 表示失败（无权限 / IO 异常）
     */
    suspend fun save(context: Context, bitmap: Bitmap, displayName: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { insertToGallery(context, bitmap, displayName) }.getOrDefault(false)
        }

    private fun insertToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 分区存储：只声明相对路径，系统负责真实落盘位置
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 写入完成再取消 pending，避免相册里出现半张图
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            true
        } catch (error: Exception) {
            // 写失败要删掉空条目，否则相册里会留下一个打不开的缩略图
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }

    /** Android 9 及以下保存相册需要该权限；10+ 不需要 */
    fun needsLegacyStoragePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** 备用：直接写公共 Pictures 目录（此版本未使用，保留给后续「导出到指定目录」） */
    @Suppress("unused")
    fun legacyPicturesFile(displayName: String): File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "$ALBUM_NAME/$displayName",
    )
}