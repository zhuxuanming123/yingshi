package com.b8.movietracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
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
 *   1. 处理通知/桌面小组件点击跳转（EXTRA_MOVIE_ID），App 在前台时走 onNewIntent；
 *   2. 启动时先读 DataStore 配置，配置就绪后再结束启动画面 —— 保证深色模式
 *      "首帧即深色"，不会先闪一下浅色界面；
 *   3. 把主题模式交给 Compose 主题，后续在设置页切换时实时重组。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        /** 通知/小组件点击跳转详情页时传递的影片 ID key */
        const val EXTRA_MOVIE_ID = "movie_id"
        /** 未携带影片 ID 的启动 */
        private const val NO_MOVIE_ID = -1L
    }

    private val viewModel: MainViewModel by viewModels()

    /** 待跳转的影片 id；null 表示普通启动 */
    private val pendingMovieId = MutableStateFlow<Long?>(null)

    /**
     * 跳转请求序号：同一部影片被连续点击时 movieId 不变，
     * 靠这个自增的序号让 Compose 侧的 LaunchedEffect 再次触发。
     */
    private val navigationRequest = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 必须在 super.onCreate() 之前安装，才能接管启动画面
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 配置没读出来之前一直显示启动画面（通常只有几毫秒）
        splashScreen.setKeepOnScreenCondition { !viewModel.isSettingsLoaded }

        // 冷启动：读取通知/小组件传来的 movieId
        pendingMovieId.value = intent.readMovieId()

        setContent {
            // 配置未就绪时不渲染内容（此时启动画面还在），就绪后按真实主题渲染首帧
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val movieId by pendingMovieId.collectAsStateWithLifecycle()
            val requestId by navigationRequest.collectAsStateWithLifecycle()
            B8MovieTrackerTheme(themeMode = settings?.themeMode ?: ThemeMode.SYSTEM) {
                MovieTrackerApp(initialMovieId = movieId, navigationRequest = requestId)
            }
        }
    }

    /**
     * App 已经在运行（例如从桌面小组件点进来）时不会重建 Activity，
     * 只会回调这里；必须在这里接管新的 movieId，否则会停留在原来的页面。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val movieId = intent.readMovieId() ?: return
        pendingMovieId.value = movieId
        navigationRequest.value += 1
    }

    private fun Intent.readMovieId(): Long? =
        getLongExtra(EXTRA_MOVIE_ID, NO_MOVIE_ID).takeIf { it != NO_MOVIE_ID }
}