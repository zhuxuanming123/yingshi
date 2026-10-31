package com.b8.movietracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.b8.movietracker.domain.model.Movie
import java.util.Locale

/**
 * 影片列表项（首页 / 搜索 / 我的片单 三处复用）。
 * 展示：海报缩略图 + 标题 + 上映年份 + 评分。
 *
 * 阶段4：海报改用 PosterImage 组件，统一占位/错误容错。
 */
@Composable
fun MovieListItem(
    movie: Movie,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 海报缩略图（PosterImage 统一容错）
        PosterImage(
            posterUrl = movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier
                .size(width = 80.dp, height = 120.dp)
                .clip(RoundedCornerShape(8.dp)),
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            if (movie.releaseDate != null) {
                Text(
                    text = "上映：${movie.releaseDate.take(4)} 年",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⭐", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", movie.voteAverage),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF57C00),
                    fontWeight = FontWeight.Bold,
                )
                // 用户评分（标记已看时显示）
                if (movie.userRating != null) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "我的评分：${movie.userRating!!.toInt()} 分",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 短评（标记已看时显示）
            if (!movie.review.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = movie.review,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
