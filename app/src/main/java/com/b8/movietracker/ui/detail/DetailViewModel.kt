package com.b8.movietracker.ui.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.b8.movietracker.domain.model.Movie
import com.b8.movietracker.domain.repository.FavoriteRepository
import com.b8.movietracker.domain.repository.MovieRepository
import com.b8.movietracker.ui.common.UiState
import com.b8.movietracker.util.GallerySaver
import com.b8.movietracker.util.ShareCardGenerator
import com.b8.movietracker.util.ShareCardStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * 详情 ViewModel：详情 + 收藏状态（想看/已看）+ 预告片 + 观影记录 + 分享卡片。
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val favoriteRepository: FavoriteRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: Long = savedStateHandle.get<Long>("movieId") ?: -1L

    /** 详情数据状态 */
    private val _uiState = MutableStateFlow<UiState<Movie>>(UiState.Loading)
    val uiState: StateFlow<UiState<Movie>> = _uiState.asStateFlow()

    /** 是否在「想看」列表 */
    private val _isWantToWatch = MutableStateFlow(false)
    val isWantToWatch: StateFlow<Boolean> = _isWantToWatch.asStateFlow()

    /** 是否在「已看」列表 */
    private val _isWatched = MutableStateFlow(false)
    val isWatched: StateFlow<Boolean> = _isWatched.asStateFlow()

    /** 预告片 YouTube key */
    private val _trailerKey = MutableStateFlow<String?>(null)
    val trailerKey: StateFlow<String?> = _trailerKey.asStateFlow()

    /** 分享卡片状态（Idle / Generating / Ready / Error） */
    private val _shareCardState = MutableStateFlow<ShareCardState>(ShareCardState.Idle)
    val shareCardState: StateFlow<ShareCardState> = _shareCardState.asStateFlow()

    /** 一次性的用户提示（保存结果等），显示后由界面层调 [clearUserMessage] 清空 */
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    /** 是否正在生成，避免连点生成多张 */
    private val isGenerating: Boolean
        get() = _shareCardState.value is ShareCardState.Generating

    init {
        loadDetail()
        observeWatchStatus()
        loadTrailer()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            movieRepository.getMovieDetail(movieId)
                .catch { error ->
                    _uiState.value = UiState.Error(error.message ?: "加载失败")
                }
                .collect { movie ->
                    _uiState.value = UiState.Success(movie)
                }
        }
    }

    /** 观察「想看」「已看」两个列表，更新按钮状态 */
    private fun observeWatchStatus() {
        viewModelScope.launch {
            favoriteRepository.getMoviesByStatus(FavoriteRepository.STATUS_WANT_TO_WATCH)
                .collect { list -> _isWantToWatch.value = list.any { it.id == movieId } }
        }
        viewModelScope.launch {
            favoriteRepository.getMoviesByStatus(FavoriteRepository.STATUS_WATCHED)
                .collect { list -> _isWatched.value = list.any { it.id == movieId } }
        }
    }

    private fun loadTrailer() {
        viewModelScope.launch {
            // 预告片属于次要信息：断网时不应让整个 ViewModel 崩溃（会直接把进程带崩），
            // 失败就静默降级为「暂无预告片」。
            movieRepository.getMovieTrailer(movieId)
                .catch { _trailerKey.value = null }
                .collect { key ->
                    _trailerKey.value = key
                }
        }
    }

    /** 切换「想看」 */
    fun toggleWantToWatch() {
        val movie = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(movie)
        }
    }

    /** 标记「已看」：写入评分 + 短评 + 观影日期 */
    fun markWatched(rating: Double?, review: String?) {
        val movie = (_uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            favoriteRepository.markWatched(
                movie.copy(
                    userRating = rating,
                    watchedDate = System.currentTimeMillis(),
                    review = review,
                )
            )
        }
    }

    // ===================== 分享卡片 =====================

    /**
     * 生成分享卡片。
     *
     * 合成与写文件都在 [viewModelScope] 的后台线程执行（生成器内部已切到 Default/IO），
     * 界面只通过 [shareCardState] 展示 Generating 加载态，主线程不做任何重活。
     */
    fun generateShareCard() {
        if (isGenerating) return
        val movie = (_uiState.value as? UiState.Success)?.data ?: return

        viewModelScope.launch {
            _shareCardState.value = ShareCardState.Generating
            runCatching {
                val bitmap = ShareCardGenerator.generate(context, movie)
                val file = ShareCardStore.newCacheFile(context, movie.id)
                ShareCardStore.write(bitmap, file)
                ShareCardState.Ready(bitmap, file)
            }.onSuccess { ready ->
                _shareCardState.value = ready
            }.onFailure { error ->
                _shareCardState.value = ShareCardState.Error(
                    error.message?.takeIf { it.isNotBlank() } ?: "分享卡片生成失败，请重试",
                )
            }
        }
    }

    /** 关闭预览 / 取消生成 */
    fun dismissShareCard() {
        _shareCardState.value = ShareCardState.Idle
    }

    /**
     * 保存当前卡片到相册。
     *
     * 只有界面层已经确认权限（Android 9 及以下）时才会调用；
     * 保存失败只提示，不影响卡片预览与系统分享。
     */
    fun saveShareCard() {
        val ready = _shareCardState.value as? ShareCardState.Ready ?: return
        viewModelScope.launch {
            val displayName = String.format(
                Locale.US,
                "B8_%d_%d.png",
                movieId,
                System.currentTimeMillis(),
            )
            val saved = GallerySaver.save(context, ready.bitmap, displayName)
            _userMessage.value = if (saved) "已保存到相册" else "保存失败，请检查存储空间后重试"
        }
    }

    /** 存储权限被拒绝时的降级提示：分享照常可用 */
    fun notifyStoragePermissionDenied() {
        _userMessage.value = "未授予存储权限，已跳过保存；仍可通过「分享」发送卡片"
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
