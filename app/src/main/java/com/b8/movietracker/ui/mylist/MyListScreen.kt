package com.b8.movietracker.ui.mylist

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.domain.model.Genre
import com.b8.movietracker.domain.statistics.WatchStatistics
import com.b8.movietracker.notification.NotificationHelper
import com.b8.movietracker.ui.common.UiStateView
import com.b8.movietracker.ui.components.MovieListItem
import com.b8.movietracker.worker.ReleaseReminderScheduler
import java.util.Locale

/**
 * 我的片单页：想看/已看 Tab + 观影统计 + 上映提醒。
 */
@Composable
fun MyListScreen(
    onMovieClick: (Long) -> Unit,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val wantToWatch by viewModel.wantToWatch.collectAsStateWithLifecycle()
    val watched by viewModel.watched.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 当前选中的 Tab：0=想看，1=已看
    var selectedTab by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReleaseReminderScheduler.schedule(context)
            Toast.makeText(context, "上映提醒已开启", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "通知权限被拒绝，无法开启提醒", Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "我的片单",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )

        // 想看 / 已看 Tab（任务3）
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("想看") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("已看") },
            )
        }

        // 已看 Tab 显示观影统计（任务4/5）
        if (selectedTab == 1) {
            StatsCard(statistics = statistics, genres = genres)
        }

        // 开启上映提醒按钮
        OutlinedButton(
            onClick = {
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

        // 根据 Tab 展示对应列表
        val currentState = if (selectedTab == 0) wantToWatch else watched
        UiStateView(
            state = currentState,
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

/** 观影统计卡片 */
@Composable
private fun StatsCard(statistics: WatchStatistics, genres: List<Genre>) {
    val genreNameMap = remember(genres) { genres.associate { it.id to it.name } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(value = "${statistics.totalWatched}", label = "已看总数")
                StatItem(value = String.format(Locale.US, "%.1f", statistics.averageRating), label = "平均评分")
                StatItem(value = "${statistics.watchedThisMonth}", label = "本月观影")
            }

            // 类型分布（top 3）
            if (statistics.genreDistribution.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(text = "类型分布", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                statistics.genreDistribution.entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .forEach { (genreId, count) ->
                        Text(
                            text = "${genreNameMap[genreId] ?: "其他"}：$count 部",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
