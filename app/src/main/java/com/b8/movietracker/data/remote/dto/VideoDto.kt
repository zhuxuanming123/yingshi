package com.b8.movietracker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 视频 DTO（TMDB `/movie/{id}/videos` 接口返回）。
 *
 * TMDB 的视频大多是 YouTube 的预告片，关键字段：
 *   key  —— YouTube 视频 ID（拼到 youtube.com/watch?v= 后面）
 *   type —— 类型：Trailer（预告片）/ Teaser / Clip ...
 *   site —— 站点：YouTube / Vimeo ...
 */
data class VideoDto(
    @SerializedName("key") val key: String,
    @SerializedName("name") val name: String?,
    @SerializedName("site") val site: String?,
    @SerializedName("type") val type: String?,
)

/** 视频列表响应 */
data class VideoListResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("results") val results: List<VideoDto> = emptyList(),
)
