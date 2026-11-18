package com.b8.movietracker.widget.p1.data

/**
 * 小组件需要的一条「即将上映」影片。
 *
 * 字段与主 App 的 ContentProvider 列名一一对应（见 MovieProviderContract），
 * 这里只保留组件展示要用的 4 个字段，避免跨进程传输多余数据。
 */
data class UpcomingMovie(
    val id: Long,
    val title: String,
    val posterUrl: String?,
    val releaseDate: String?,
)

/**
 * 数据来源，用于空态时给出不同的提示文案。
 */
enum class Source {
    /** 正常从主 App 读到了数据 */
    MAIN_APP,

    /** 主 App 未安装 / Provider 不存在 / 查询异常 */
    UNAVAILABLE,

    /** 主 App 在，但没有授予读取权限（通常是先装了小组件、后装主 App） */
    DENIED,

    /** 演示页的示例数据 */
    SAMPLE,
}

/**
 * 一次读取的结果：待映总数 + 前几条影片 + 数据来源。
 */
data class WidgetData(
    val upcomingCount: Int,
    val movies: List<UpcomingMovie>,
    val source: Source,
) {
    val hasData: Boolean get() = movies.isNotEmpty()
}