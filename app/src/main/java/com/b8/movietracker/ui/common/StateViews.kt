package com.b8.movietracker.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 三种通用状态视图，供所有页面复用。
 * 目的：避免每个页面重复写加载圈 / 空提示 / 错误提示，统一视觉与交互。
 */

/** 加载态：居中转圈 */
@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** 空态：居中提示无数据 */
@Composable
fun EmptyView(
    message: String = "暂无数据",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "📭", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** 错误态：居中提示错误 + 重试按钮 */
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "⚠️", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("重试")
            }
        }
    }
}

/**
 * 根据 UiState 统一渲染（列表/详情场景通用）。
 * 用法：
 *   UiStateView(state = uiState, onRetry = viewModel::reload) { movies ->
 *       LazyColumn { ... }   // 仅在 Success 时渲染
 *   }
 *
 * @param state 当前 UI 状态
 * @param onRetry 错误态点击重试的回调
 * @param content 仅 Success 状态时执行，参数为携带的数据
 */
@Composable
fun <T> UiStateView(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is UiState.Loading -> LoadingView(modifier)
        is UiState.Empty -> EmptyView(modifier = modifier)
        is UiState.Error -> ErrorView(state.message, onRetry, modifier)
        is UiState.Success -> content(state.data)
    }
}
