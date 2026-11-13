package com.b8.movietracker.ui.settings

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.b8.movietracker.BuildConfig
import com.b8.movietracker.domain.model.SortOption
import com.b8.movietracker.domain.model.ThemeMode
import com.b8.movietracker.notification.NotificationHelper
import com.b8.movietracker.util.ImageCacheManager

/**
 * 设置中心页面（阶段：设置中心 + DataStore）。
 *
 * 四个分区：
 *   1. 外观        —— 跟随系统 / 浅色 / 深色（切换即时生效，无需重启）
 *   2. 上映提醒    —— 总开关 + 每日提醒时间（写入后 WorkManager 立即重排任务）
 *   3. 默认筛选    —— 排序方式 / 最低评分 / 影片类型（首页启动时自动应用）
 *   4. 存储        —— 图片缓存大小查看与一键清理
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }

    // 开启提醒前先申请通知权限（Android 13+），避免"开关是开的但通知发不出来"
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setReminderEnabled(true)
        } else {
            Toast.makeText(context, "通知权限被拒绝，无法开启上映提醒", Toast.LENGTH_LONG).show()
        }
    }

    // 进入页面时统计一次缓存大小
    LaunchedEffect(Unit) { viewModel.refreshCacheSize() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        // ---------- 1. 外观 ----------
        SectionTitle("外观")
        SettingsCard {
            ThemeMode.entries.forEachIndexed { index, mode ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            role = Role.RadioButton,
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.themeMode == mode,
                        onClick = null, // 点击交给整行处理，扩大触控区域
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // ---------- 2. 上映提醒 ----------
        SectionTitle("上映提醒")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("开启上映提醒", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (settings.reminderEnabled) {
                            "每天 ${settings.reminderTimeLabel} 检查片单中 7 天内上映的影片"
                        } else {
                            "关闭后不再发送上映通知"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.reminderEnabled,
                    onCheckedChange = { enabled ->
                        when {
                            // 关闭：直接写配置，Application 会取消周期任务
                            !enabled -> viewModel.setReminderEnabled(false)
                            // 开启：Android 13+ 且尚未授权时先要权限，授权成功再打开开关
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !NotificationHelper.hasPermission(context) ->
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            else -> viewModel.setReminderEnabled(true)
                        }
                    },
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("每日提醒时间", modifier = Modifier.weight(1f))
                Text(
                    text = settings.reminderTimeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = "提醒时间修改后，后台任务会自动重排；系统省电策略下可能有几分钟误差。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )

        // ---------- 3. 默认筛选偏好 ----------
        SectionTitle("默认筛选偏好")
        SettingsCard {
            LabeledBlock("排序方式") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SortOption.entries.forEach { option ->
                        FilterChip(
                            selected = settings.defaultSortBy == option,
                            onClick = { viewModel.setDefaultSort(option) },
                            label = { Text(option.label) },
                        )
                    }
                }
            }
            HorizontalDivider()
            LabeledBlock("最低评分") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(null, 6.0, 7.0, 8.0).forEach { vote ->
                        FilterChip(
                            selected = settings.defaultMinVote == vote,
                            onClick = { viewModel.setDefaultMinVote(vote) },
                            label = { Text(vote?.let { "${it.toInt()} 分以上" } ?: "不限") },
                        )
                    }
                }
            }
            HorizontalDivider()
            LabeledBlock("影片类型") {
                if (genres.isEmpty()) {
                    Text(
                        text = "类型加载中…（需要网络）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        genres.forEach { genre ->
                            FilterChip(
                                selected = genre.id in settings.defaultGenreIds,
                                onClick = { viewModel.toggleDefaultGenre(genre.id) },
                                label = { Text(genre.name) },
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "首页打开时会自动套用这里的筛选条件（年份范围属于临时筛选，不保存）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )

        // ---------- 4. 存储 ----------
        SectionTitle("存储")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("图片缓存", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = ImageCacheManager.formatSize(cacheSizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = {
                        viewModel.clearImageCache()
                        Toast.makeText(context, "图片缓存已清理", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text("一键清理")
                }
            }
        }

        // ---------- 5. 关于 ----------
        SectionTitle("关于")
        SettingsCard {
            InfoRow("应用版本", BuildConfig.VERSION_NAME)
            HorizontalDivider()
            InfoRow("数据来源", "TMDB")
            HorizontalDivider()
            InfoRow("配置存储", "Jetpack DataStore")
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            onConfirm = { hour, minute ->
                viewModel.setReminderTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

/** 分组标题 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** 统一的卡片容器 */
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

/** 「标题 + 一组选项」的区块 */
@Composable
private fun LabeledBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

/** 只读信息行 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 每日提醒时间选择弹窗 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每日提醒时间") },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}