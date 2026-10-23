package com.b8.movietracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.b8.movietracker.ui.navigation.MovieTrackerApp
import com.b8.movietracker.ui.theme.B8MovieTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主入口 Activity。
 *
 * 阶段5 新增：处理通知点击跳转。
 *   - 通知的 PendingIntent 携带 EXTRA_MOVIE_ID
 *   - 这里读取它，传给 MovieTrackerApp，由导航自动打开对应影片详情页
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        /** 通知点击跳转详情页时传递的影片 ID key */
        const val EXTRA_MOVIE_ID = "movie_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 读取通知传来的 movieId（-1 表示非通知启动，即正常启动）
        val movieId = intent.getLongExtra(EXTRA_MOVIE_ID, -1L).takeIf { it != -1L }

        setContent {
            B8MovieTrackerTheme {
                MovieTrackerApp(initialMovieId = movieId)
            }
        }
    }
}
