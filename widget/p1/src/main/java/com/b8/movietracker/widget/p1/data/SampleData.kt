package com.b8.movietracker.widget.p1.data

/**
 * 演示页专用的示例数据。
 *
 * 作用：主 App 没安装、或片单为空时，依然能在演示页看到两款组件的「有数据」形态，
 * 保证模块独立可演示。注意：示例数据只用于演示页预览，不会写进桌面组件。
 */
object SampleData {

    fun widgetData(): WidgetData = WidgetData(
        upcomingCount = 5,
        movies = listOf(
            UpcomingMovie(id = 900001L, title = "星际回声", posterUrl = null, releaseDate = "2026-12-18"),
            UpcomingMovie(id = 900002L, title = "雨夜书店", posterUrl = null, releaseDate = "2026-12-25"),
            UpcomingMovie(id = 900003L, title = "钢铁防线", posterUrl = null, releaseDate = "2027-01-01"),
        ),
        source = Source.SAMPLE,
    )
}