package com.b8.movietracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.domain.model.ThemeMode
import com.b8.movietracker.ui.navigation.MovieTrackerApp
import com.b8.movietracker.ui.theme.B8MovieTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主入口 Activity。
 *
 * 职责：
 *   1. 处理通知点击跳转（EXTRA_MOVIE_ID）；
 *   2. 启动时先读 DataStore 配置，配置就绪后再结束启动画面 —— 保证深色模式
 *      "首帧即深色"，不会先闪一下浅色界面；
 *   3. 把主题模式交给 Compose 主题，后续在设置页切换时实时重组。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        /** 通知点击跳转详情页时传递的影片 ID key */
        const val EXTRA_MOVIE_ID = "movie_id"
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 必须在 super.onCreate() 之前安装，才能接管启动画面
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 配置没读出来之前一直显示启动画面（通常只有几毫秒）
        splashScreen.setKeepOnScreenCondition { !viewModel.isSettingsLoaded }

        // 读取通知传来的 movieId（-1 表示非通知启动，即正常启动）
        val movieId = intent.getLongExtra(EXTRA_MOVIE_ID, -1L).takeIf { it != -1L }

        setContent {
            // 配置未就绪时不渲染内容（此时启动画面还在），就绪后按真实主题渲染首帧
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            B8MovieTrackerTheme(themeMode = settings?.themeMode ?: ThemeMode.SYSTEM) {
                MovieTrackerApp(initialMovieId = movieId)
            }
        }
    }
}