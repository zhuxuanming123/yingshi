package com.b8.movietracker.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 分享卡片的临时文件存储。
 *
 * 为什么放 cacheDir/share：
 * - cacheDir 不需要任何存储权限，Android 10+ 的分区存储也不影响；
 * - FileProvider 只把 `share/` 子目录写进白名单（见 res/xml/file_paths.xml），
 *   不会把整个缓存目录暴露给第三方应用；
 * - 系统清理缓存时文件自然回收，不用自己写过期删除逻辑。
 */
object ShareCardStore {

    /** 与 AndroidManifest 中 provider 的 authorities 保持一致 */
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    private const val SHARE_DIR = "share"

    /** 生成一个本次分享专用、不会互相覆盖的文件名 */
    fun newCacheFile(context: Context, movieId: Long): File {
        val dir = File(context.cacheDir, SHARE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "b8_movie_${movieId}_${System.currentTimeMillis()}.png")
    }

    /** 把位图写成 PNG，返回写好的文件（IO 线程） */
    suspend fun write(bitmap: Bitmap, file: File): File = withContext(Dispatchers.IO) {
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        file
    }

    /** 转成 content:// URI，配合 FLAG_GRANT_READ_URI_PERMISSION 即可跨进程分享 */
    fun uri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
}