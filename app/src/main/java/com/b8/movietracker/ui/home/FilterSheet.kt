package com.b8.movietracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.b8.movietracker.domain.model.FilterCriteria
import com.b8.movietracker.domain.model.Genre
import com.b8.movietracker.domain.model.SortOption

/**
 * 首页筛选面板（任务1：多维度组合筛选）。
 *
 * 展示四类条件：类型（多选）、评分下限（单选）、上映年份范围、排序方式（单选）。
 * 用户在面板内选择，点「应用」后把 FilterCriteria 回调给 ViewModel。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    genres: List<Genre>,
    currentCriteria: FilterCriteria,
    onApply: (FilterCriteria) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    // 临时筛选状态（应用前不生效）
    var selectedGenres by remember { mutableStateOf(currentCriteria.genreIds.toSet()) }
    var minVote by remember { mutableStateOf(currentCriteria.minVote) }
    var startYear by remember {
        mutableStateOf(currentCriteria.yearRange?.first?.toString() ?: "")
    }
    var endYear by remember {
        mutableStateOf(currentCriteria.yearRange?.last?.toString() ?: "")
    }
    var sortBy by remember { mutableStateOf(currentCriteria.sortBy) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(text = "筛选影片", style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(16.dp))

            // 类型（多选）
            Text(text = "类型", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                genres.forEach { genre ->
                    FilterChip(
                        selected = genre.id in selectedGenres,
                        onClick = {
                            selectedGenres =
                                if (genre.id in selectedGenres) {
                                    selectedGenres - genre.id
                                } else {
                                    selectedGenres + genre.id
                                }
                        },
                        label = { Text(genre.name) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 评分下限（单选，可取消）
            Text(text = "评分", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf<Double?>(null, 6.0, 7.0, 8.0).forEach { vote ->
                    FilterChip(
                        selected = minVote == vote,
                        onClick = { minVote = vote },
                        label = { Text(vote?.let { "${it.toInt()}分+" } ?: "不限") },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 上映年份范围
            Text(text = "上映年份", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = startYear,
                    onValueChange = { startYear = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("起始年") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text("—")
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = endYear,
                    onValueChange = { endYear = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("结束年") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 排序（单选）
            Text(text = "排序方式", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOption.entries.forEach { option ->
                    FilterChip(
                        selected = sortBy == option,
                        onClick = { sortBy = option },
                        label = { Text(option.label) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 底部按钮
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        // 重置为默认
                        selectedGenres = emptySet()
                        minVote = null
                        startYear = ""
                        endYear = ""
                        sortBy = SortOption.POPULARITY_DESC
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("重置")
                }
                Button(
                    onClick = {
                        val criteria = FilterCriteria(
                            genreIds = selectedGenres.toList(),
                            minVote = minVote,
                            yearRange = parseYearRange(startYear, endYear),
                            sortBy = sortBy,
                        )
                        onApply(criteria)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("应用")
                }
            }
        }
    }
}

/** 把两个年份文本解析成 IntRange（任一为空或非法则返回 null = 不限） */
private fun parseYearRange(startYear: String, endYear: String): IntRange? {
    val start = startYear.toIntOrNull()
    val end = endYear.toIntOrNull()
    return if (start != null && end != null && start <= end) start..end else null
}
