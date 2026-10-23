package com.b8.movietracker.ui.mylist

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.notification.NotificationHelper
import com.b8.movietracker.ui.common.UiStateView
import com.b8.movietracker.ui.components.MovieListItem
import com.b8.movietracker.worker.ReleaseReminderScheduler

/**
 * 我的片单页：统计卡片 + 上映提醒开关 + 收藏列表。
 */
@Composable
fun MyListScreen(
    onMovieClick: (Long) -> Unit,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 通知权限请求启动器（任务4）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReleaseReminderScheduler.schedule(context)
            Toast.makeText(context, "上映提醒已开启", Toast.LENGTH_SHORT).show()
        } else {
            // 用户拒绝：不崩溃，给降级提示
            Toast.makeText(context, "通知权限被拒绝，无法开启提醒", Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "我的片单",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )

        // 统计卡片（任务5：片单总数 + 即将上映数量）
        StatsCard(stats = stats)

        // 开启上映提醒按钮
        OutlinedButton(
            onClick = {
                // Android 13+ 需要运行时申请通知权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationHelper.hasPermission(context)
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    ReleaseReminderScheduler.schedule(context)
                    Toast.makeText(context, "上映提醒已开启", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("🔔 开启上映提醒")
        }

        UiStateView(
            state = uiState,
            onRetry = { /* 响应式数据源，无需手动重试 */ },
        ) { movies ->
            LazyColumn {
                items(movies, key = { it.id }) { movie ->
                    MovieListItem(movie = movie, onClick = { onMovieClick(movie.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

/** 片单统计卡片 */
@Composable
private fun StatsCard(stats: MyListStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${stats.total}", style = MaterialTheme.typography.headlineMedium)
                Text(text = "片单总数", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${stats.upcoming}", style = MaterialTheme.typography.headlineMedium)
                Text(text = "即将上映", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
