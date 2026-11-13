package com.b8.movietracker.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.b8.movietracker.domain.model.ThemeMode

/**
 * 浅色配色。
 * 暂不启用动态取色（Android 12+），保证各版本观感一致。
 */
private val LightColors = lightColorScheme()

/**
 * 深色配色。
 * 使用 Material3 默认深色基线，后续如需品牌色可在此统一调整。
 */
private val DarkColors = darkColorScheme()

/**
 * 应用 Compose 主题。
 *
 * @param themeMode 主题模式（跟随系统 / 浅色 / 深色），来自 DataStore 配置。
 *   MainActivity 在启动时先读到配置再渲染首帧，因此切换主题时不会先亮后暗；
 *   运行中切换只是重组 MaterialTheme，不会重建 Activity，也就没有闪烁。
 */
@Composable
fun B8MovieTrackerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // 让系统状态栏 / 导航栏也跟随主题：
    // Compose 只负责应用内部绘制，系统的栏默认跟着"系统深浅色"走。
    // 不处理的话，在「App 深色 + 系统浅色」时顶部会留一条白边，深色模式显得不完整。
    val view = LocalView.current
    if (!view.isInEditMode) {
        // 记录已应用的背景色，避免每次重组都重建 Window 背景
        val appliedBackground = remember { IntArray(1) { Int.MIN_VALUE } }
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val background = colorScheme.background.toArgb()
            if (appliedBackground[0] != background) {
                appliedBackground[0] = background
                activity.applySystemBarBackground(background)
            }
            // 浅色主题配深色图标，深色主题配浅色图标
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

/** 兼容 ContextThemeWrapper 的 Activity 查找（直接用 `context as Activity` 在部分场景会崩） */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 把状态栏 / 导航栏刷成主题背景色。
 *
 * `statusBarColor` 在 API 35 起已被系统忽略（强制 edge-to-edge），
 * 但低版本（Android 14 及以下）仍然依赖它，所以保留并抑制过时警告。
 */
@Suppress("DEPRECATION")
private fun Activity.applySystemBarBackground(background: Int) {
    window.setBackgroundDrawable(ColorDrawable(background))
    window.statusBarColor = background
    window.navigationBarColor = background
}