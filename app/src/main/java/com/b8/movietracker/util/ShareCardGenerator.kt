package com.b8.movietracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.b8.movietracker.domain.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

/**
 * 分享卡片生成器：用 Canvas 把海报、标题、评分、简介和 App 水印合成一张位图。
 *
 * 设计要点：
 * - 输出 1080×1440（3:4 竖版），正方形/9:16 在聊天软件里缩略图容易糊，3:4 是分享卡片常用比例；
 * - 全部绘制在软件 Canvas 上，不依赖 Compose 测量，也不受屏幕密度影响；
 * - 海报用 Coil 加载（命中磁盘缓存，不额外发请求），加圆角 + 居中裁剪；
 * - 文本用 [StaticLayout] 排版，自动换行 + 超出行数省略号，避免长简介把卡片撑破。
 */
object ShareCardGenerator {

    /** 卡片宽度（px） */
    const val WIDTH = 1080

    /** 卡片高度（px） */
    const val HEIGHT = 1440

    private const val PADDING = 72f
    private const val POSTER_LEFT = PADDING
    private const val POSTER_TOP = 96f
    private const val POSTER_WIDTH = 384f
    private const val POSTER_HEIGHT = 576f
    private const val POSTER_RADIUS = 28f
    private const val CONTENT_LEFT = POSTER_LEFT + POSTER_WIDTH + 64f

    /** 右侧文案区宽度（px，整数给 StaticLayout 用） */
    private val CONTENT_WIDTH = (WIDTH - CONTENT_LEFT - PADDING).toInt()

    private const val COLOR_BG_TOP = 0xFF1C1B29.toInt()
    private const val COLOR_BG_MID = 0xFF121019.toInt()
    private const val COLOR_BG_BOTTOM = 0xFF07070B.toInt()
    private const val COLOR_TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    private const val COLOR_TEXT_SECONDARY = 0xFF9A9AB0.toInt()
    private const val COLOR_TEXT_BODY = 0xFFC9C9D6.toInt()
    private const val COLOR_ACCENT = 0xFFFF6B6B.toInt()
    private const val COLOR_RATING = 0xFFFFB300.toInt()

    /**
     * 生成分享卡片位图。
     *
     * 悬挂函数：海报解码（IO）与绘制（Default）都不在主线程执行。
     *
     * @param movie 影片信息（详情页的 [Movie] 已含海报、评分、简介、上映日期）
     * @return 已绘制完成的位图，调用方负责在合适时机 recycle
     */
    suspend fun generate(context: Context, movie: Movie): Bitmap {
        val poster = loadPoster(context, movie.posterUrl)
        return withContext(Dispatchers.Default) { drawCard(movie, poster) }
    }

    /** 用 Coil 加载海报：优先磁盘/内存缓存，避免分享时再走一次网络 */
    private suspend fun loadPoster(context: Context, posterUrl: String?): Bitmap? {
        if (posterUrl.isNullOrBlank()) return null
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(posterUrl)
                .size(800, 1200)
                .allowHardware(false) // 软件 Canvas 需要可读像素
                .build()
            (context.imageLoader.execute(request) as? SuccessResult)
                ?.drawable
                ?.toBitmap()
        }.getOrNull()
    }

    private fun drawCard(movie: Movie, poster: Bitmap?): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawPoster(canvas, poster)

        // 右侧信息区：标题 → 评分 → 上映日期
        var cursorY = POSTER_TOP + 8f
        cursorY = drawWrappedText(
            canvas = canvas,
            text = movie.title,
            paint = titlePaint(),
            left = CONTENT_LEFT,
            top = cursorY,
            width = CONTENT_WIDTH,
            maxLines = 3,
        ) + 28f
        cursorY = drawRating(canvas, movie.voteAverage, CONTENT_LEFT, cursorY) + 22f
        drawReleaseDate(canvas, movie.releaseDate, CONTENT_LEFT, cursorY)

        // 下方简介区
        val sectionTop = POSTER_TOP + POSTER_HEIGHT + 64f
        drawSectionTitle(canvas, "简介", sectionTop)
        val overview = movie.overview?.takeIf { it.isNotBlank() } ?: "暂无简介"
        drawWrappedText(
            canvas = canvas,
            text = overview,
            paint = bodyPaint(),
            left = PADDING,
            top = sectionTop + 54f,
            width = (WIDTH - PADDING * 2).toInt(),
            maxLines = 6,
        )

        drawFooter(canvas)
        return bitmap
    }

    /** 深色渐变底 + 右上角柔光圆 + 顶部强调色条，比纯灰底更像「卡片」而不是截图 */
    private fun drawBackground(canvas: Canvas) {
        val gradient = LinearGradient(
            0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
            intArrayOf(COLOR_BG_TOP, COLOR_BG_MID, COLOR_BG_BOTTOM),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), Paint().apply { shader = gradient })

        canvas.drawCircle(
            WIDTH - 60f, 120f, 320f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x14FF6B6B },
        )

        val accent = LinearGradient(
            0f, 0f, WIDTH.toFloat(), 0f,
            intArrayOf(COLOR_ACCENT, 0xFFFF9F43.toInt(), 0x00FFFFFF),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), 8f, Paint().apply { shader = accent })
    }

    private fun drawPoster(canvas: Canvas, poster: Bitmap?) {
        val rect = RectF(POSTER_LEFT, POSTER_TOP, POSTER_LEFT + POSTER_WIDTH, POSTER_TOP + POSTER_HEIGHT)

        if (poster == null) {
            val placeholder = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                intArrayOf(0xFF2A2836.toInt(), 0xFF191722.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(rect, POSTER_RADIUS, POSTER_RADIUS, Paint().apply { shader = placeholder })
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF6E6E80.toInt()
                textSize = 30f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("暂无海报", rect.centerX(), rect.centerY(), textPaint)
        } else {
            val shader = BitmapShader(poster, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(centerCropMatrix(poster, rect))
            }
            canvas.drawRoundRect(rect, POSTER_RADIUS, POSTER_RADIUS, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        }

        // 半透明白描边，让海报从深色底上「浮」起来
        canvas.drawRoundRect(
            rect, POSTER_RADIUS, POSTER_RADIUS,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = 0x33FFFFFF
            },
        )
    }

    /** 居中裁剪：保证海报不变形地铺满目标区域 */
    private fun centerCropMatrix(source: Bitmap, target: RectF): Matrix {
        val scale = max(target.width() / source.width, target.height() / source.height)
        val dx = target.left + (target.width() - source.width * scale) / 2f
        val dy = target.top + (target.height() - source.height * scale) / 2f
        return Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
    }

    private fun drawRating(canvas: Canvas, voteAverage: Double, left: Float, top: Float): Float {
        val starPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_RATING
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val scorePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_RATING
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val suffixPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 30f
        }

        val baseline = top + 58f
        canvas.drawText("★", left, baseline, starPaint)
        val starWidth = starPaint.measureText("★")
        val score = String.format(Locale.US, "%.1f", voteAverage)
        canvas.drawText(score, left + starWidth + 10f, baseline, scorePaint)
        val scoreWidth = scorePaint.measureText(score)
        canvas.drawText(" / 10", left + starWidth + 10f + scoreWidth, baseline, suffixPaint)

        return baseline
    }

    private fun drawReleaseDate(canvas: Canvas, releaseDate: String?, left: Float, top: Float) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 30f
        }
        val text = if (releaseDate.isNullOrBlank()) "上映日期未知" else "上映：$releaseDate"
        canvas.drawText(text, left, top + 34f, paint)
    }

    /** 章节标题：左侧强调色竖条 + 标题文字 */
    private fun drawSectionTitle(canvas: Canvas, title: String, top: Float) {
        canvas.drawRoundRect(
            RectF(PADDING, top + 4f, PADDING + 6f, top + 36f),
            3f, 3f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT },
        )
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, PADDING + 22f, top + 34f, paint)
    }

    /** 自动换行的多行文本，超出行数用省略号收尾；返回文本块底部 y */
    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        left: Float,
        top: Float,
        width: Int,
        maxLines: Int,
    ): Float {
        if (width <= 0) return top
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(10f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }

    /** 底部分隔线 + 品牌水印 */
    private fun drawFooter(canvas: Canvas) {
        val lineY = HEIGHT - 132f
        canvas.drawRect(
            PADDING, lineY, WIDTH - PADDING, lineY + 2f,
            Paint().apply { color = 0x1FFFFFFF },
        )

        val brand = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("B8 影视追踪", PADDING, lineY + 62f, brand)

        val slogan = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 26f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("数据来自 TMDB · 发现更多好电影", WIDTH - PADDING, lineY + 60f, slogan)
    }

    private fun titlePaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_PRIMARY
        textSize = 54f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun bodyPaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_TEXT_BODY
        textSize = 34f
    }
}