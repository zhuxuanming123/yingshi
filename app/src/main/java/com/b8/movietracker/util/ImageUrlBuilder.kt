package com.b8.movietracker.util

import com.b8.movietracker.data.remote.ApiConfig

/**
 * 图片 URL 拼接工具。
 *
 * TMDB 只返回相对路径（如 "/abc123.jpg"），完整地址需拼成：
 *   https://image.tmdb.org/t/p/ + 尺寸 + 相对路径
 * 例如：https://image.tmdb.org/t/p/w342/abc123.jpg
 */
object ImageUrlBuilder {

    /**
     * 拼接海报完整地址。
     * @param path 相对路径；为空或 null 时返回 null（UI 层显示占位图）
     * @param size 尺寸后缀，默认 w342
     */
    fun buildPosterUrl(
        path: String?,
        size: String = ApiConfig.ImageSize.POSTER,
    ): String? {
        if (path.isNullOrBlank()) return null
        return ApiConfig.IMAGE_BASE_URL + size + path
    }
}
