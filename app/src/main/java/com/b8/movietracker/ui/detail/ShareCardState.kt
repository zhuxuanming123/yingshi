package com.b8.movietracker.ui.detail

import android.graphics.Bitmap
import java.io.File

/**
 * 分享卡片的一次性状态机：
 * Idle -> Generating -> Ready（或 Error）-> Idle。
 *
 * 放在 ViewModel 里而不是 Compose 局部状态，好处是旋转屏幕 / 进程内重建后
 * 已经生成的卡片不会丢，用户不用重新等一次合成。
 */
sealed interface ShareCardState {

    /** 未生成，详情页处于普通浏览状态 */
    data object Idle : ShareCardState

    /** 正在后台合成位图 + 写缓存文件 */
    data object Generating : ShareCardState

    /** 生成完成：可预览、可分享、可保存 */
    data class Ready(
        val bitmap: Bitmap,
        val file: File,
    ) : ShareCardState

    /** 生成失败（海报下载失败、OOM 等），message 直接给用户看 */
    data class Error(val message: String) : ShareCardState
}