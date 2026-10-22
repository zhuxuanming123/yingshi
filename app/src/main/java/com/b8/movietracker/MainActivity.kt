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
 * 职责非常轻：只负责 setContent 挂载 Compose 根组件 MovieTrackerApp，
 * 真正的导航、页面、状态都在 MovieTrackerApp 及各自的 Screen 中。
 *
 * @AndroidEntryPoint 启用 Hilt 字段注入（当前虽未直接注入字段，
 *   但各 Screen 通过 hiltViewModel() 获取 ViewModel，仍需此注解激活 Hilt）。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            B8MovieTrackerTheme {
                MovieTrackerApp()
            }
        }
    }
}
