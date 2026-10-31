package com.b8.movietracker.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 标记「已看」弹窗（任务2：星级评分 + 填写短评）。
 *
 * @param onConfirm 确认回调，参数为 (评分, 短评)
 * @param onDismiss 取消回调
 */
@Composable
fun RatingDialog(
    onConfirm: (Double?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var rating by remember { mutableStateOf<Double?>(null) }
    var review by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("标记已看") },
        text = {
            Column {
                // 星级评分（1-10 分，用 Slider 表达）
                Text(
                    text = if (rating != null) "评分：${rating!!.toInt()} 分" else "评分：未评分",
                )
                Slider(
                    value = (rating ?: 0.0).toFloat(),
                    onValueChange = { rating = it.toDouble() },
                    valueRange = 0f..10f,
                    steps = 9, // 0~10 共 11 档
                )

                // 短评
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("短评（可选）") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(rating, review.takeIf { it.isNotBlank() })
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
