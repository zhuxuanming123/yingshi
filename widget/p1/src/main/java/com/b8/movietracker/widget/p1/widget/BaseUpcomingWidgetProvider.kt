package com.b8.movietracker.widget.p1.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.b8.movietracker.widget.p1.data.Source
import com.b8.movietracker.widget.p1.data.WidgetData
import com.b8.movietracker.widget.p1.data.WidgetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 两款组件共用的 Provider 基类，把「读数据 → 渲染 → 更新」这条链路收在一处。
 *
 * 生命周期（AppWidgetProvider 的回调都由 onReceive 分发）：
 *   - onEnabled   第一个实例被添加时调用一次
 *   - onUpdate   添加后、以及每个 updatePeriodMillis（30 分钟）和手动刷新时调用
 *   - onDeleted   删除某一实例时调用
 *   - onDisabled  最后一个实例被删除时调用
 *
 * 线程模型：onUpdate 运行在主线程，而读 Provider + 下载海报是阻塞 IO，
 * 所以用 goAsync() 把广播的生命周期延长，再切到 IO 线程完成工作，
 * 最后 finish() 通知系统「这一轮广播处理完了」。
 */
abstract class BaseUpcomingWidgetProvider : AppWidgetProvider() {

    /** 子类负责把数据渲染成自己那款组件的 RemoteViews */
    protected abstract suspend fun buildViews(context: Context, data: WidgetData): RemoteViews

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        refresh(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "${javaClass.simpleName} onEnabled：首个实例已添加")
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "${javaClass.simpleName} onDeleted：删除实例 ${appWidgetIds.joinToString()}")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "${javaClass.simpleName} onDisabled：已无实例，停止刷新")
    }

    /**
     * 后台刷新指定实例。
     *
     * 注意必须由 onReceive 的调用栈进入（onUpdate / onReceive 里），
     * 否则 goAsync() 拿不到有效的 PendingResult。
     */
    protected fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var data = WidgetRepository.load(context)
                // 开机/刚安装时系统会在主 App 的 ContentProvider 就绪之前就回调 onUpdate，
                // 此时读到的是 UNAVAILABLE。稍后重试一次，避免桌面停在空白/「未安装」状态。
                if (data.source == Source.UNAVAILABLE) {
                    delay(RETRY_DELAY_MS)
                    data = WidgetRepository.load(context)
                }
                ids.forEach { appWidgetId ->
                    val views = buildViews(context, data)
                    manager.updateAppWidget(appWidgetId, views)
                }
            } catch (t: Throwable) {
                // 小组件更新失败绝不能让桌面收到崩溃；记日志，下一轮定时刷新会重试
                Log.e(TAG, "${javaClass.simpleName} 刷新失败", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "B8Widget"
        /** 首次读取失败（Provider 尚未就绪）后的重试间隔 */
        const val RETRY_DELAY_MS = 1_500L
    }
}