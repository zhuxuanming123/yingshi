package com.b8.movietracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * 下拉刷新容器。
 *
 * material3 1.2.x 只提供了 [PullToRefreshContainer] + `nestedScrollConnection`，
 * 还没有 `PullToRefreshBox`（1.3 才有），这里做一层薄封装：
 *   - 下拉超过阈值松手 → 内部状态切到 refreshing → 回调 [onRefresh]；
 *   - [isRefreshing] 变回 false 时调用 `endRefresh()` 收起指示器；
 *   - 刷新期间保留列表内容，只有顶部指示器变化，不会闪白。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()

    // 用户松手且超过阈值后，内部状态会自动进入 refreshing，这里转成业务回调
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) onRefresh()
    }
    // 数据层刷新完成（isRefreshing=false）后收起指示器
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && state.isRefreshing) state.endRefresh()
    }

    Box(modifier.nestedScroll(state.nestedScrollConnection)) {
        content()
        // material3 1.2.x 的指示器在 progress=0 时仍会画出一个 40dp 的容器，这里只在
        // 真正下拉或刷新时挂载，避免静止状态下顶部出现一个灰色圆块
        if (state.progress > 0f || state.isRefreshing) {
            PullToRefreshContainer(
                state = state,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}