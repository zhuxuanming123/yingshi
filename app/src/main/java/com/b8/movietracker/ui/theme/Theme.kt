package com.b8.movietracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 应用 Compose 主题（阶段0 极简版）。
 * 阶段8 将补充深色模式、动态取色（Android 12+）等。
 */
@Composable
fun B8MovieTrackerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content,
    )
}
