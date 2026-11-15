package com.b8.movietracker.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 分享卡片的状态浮层：生成中 / 预览（分享、保存）/ 失败重试。
 *
 * 抽成独立 Composable 的原因：详情页主体只关心「点分享」这一件事，
 * 合成进度、预览、两个导出入口都属于分享子流程，放在一起更好维护。
 */
@Composable
fun ShareCardOverlay(
    state: ShareCardState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    when (state) {
        ShareCardState.Idle -> Unit

        ShareCardState.Generating -> Dialog(
            onDismissRequest = { /* 生成中不允许点外部关闭，避免状态悬空 */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(44.dp))
                    Text(text = "正在生成分享卡片…", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "正在合成海报、标题、评分与简介",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        is ShareCardState.Ready -> Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "分享卡片",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    // 3:4 预览：与最终导出位图同比例，所见即所得
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = state.bitmap.asImageBitmap(),
                            contentDescription = "影片分享卡片预览",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) { Text("关闭") }
                        OutlinedButton(
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("保存")
                        }
                        Button(
                            onClick = onShare,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("分享")
                        }
                    }
                }
            }
        }

        is ShareCardState.Error -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("生成失败") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = onRetry) { Text("重试") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("关闭") }
            },
        )
    }
}