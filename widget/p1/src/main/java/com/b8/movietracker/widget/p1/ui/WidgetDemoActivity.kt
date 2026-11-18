package com.b8.movietracker.widget.p1.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.b8.movietracker.widget.p1.R
import com.b8.movietracker.widget.p1.data.SampleData
import com.b8.movietracker.widget.p1.data.Source
import com.b8.movietracker.widget.p1.data.WidgetData
import com.b8.movietracker.widget.p1.data.WidgetRepository
import com.b8.movietracker.widget.p1.widget.MediumUpcomingWidgetProvider
import com.b8.movietracker.widget.p1.widget.SmallUpcomingWidgetProvider
import com.b8.movietracker.widget.p1.widget.WidgetRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 小组件模块的独立入口页（Launcher 图标点进来就是这里）。
 *
 * 三个作用：
 *   1. 证明模块能独立运行：不装主 App 也能打开、能添加组件；
 *   2. 就地预览两款组件：把真实的 RemoteViews 直接 apply 到界面上，
 *      所见即桌面所得（不是另画一套「示意图」）；
 *   3. 触发刷新 / 打开主 App，方便答辩演示。
 */
class WidgetDemoActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var renderJob: Job? = null

    private lateinit var statusView: TextView
    private lateinit var smallPreview: FrameLayout
    private lateinit var mediumPreview: FrameLayout
    private lateinit var sampleSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_demo)

        statusView = findViewById(R.id.demo_status)
        smallPreview = findViewById(R.id.demo_preview_small)
        mediumPreview = findViewById(R.id.demo_preview_medium)
        sampleSwitch = findViewById(R.id.demo_switch)

        findViewById<Button>(R.id.demo_refresh).setOnClickListener {
            requestWidgetRefresh()
            Toast.makeText(this, R.string.demo_refreshed, Toast.LENGTH_SHORT).show()
            render()
        }
        findViewById<Button>(R.id.demo_open_app).setOnClickListener { openMainApp() }
        sampleSwitch.setOnCheckedChangeListener { _, _ -> render() }

        applyWindowInsets()
    }

    /**
     * targetSdk 35 起系统强制 edge-to-edge，内容会顶到状态栏下面。
     * 这里手动把系统栏高度加成本页的内边距，并把状态栏图标改成深色（浅色背景）。
     */
    private fun applyWindowInsets() {
        val root = findViewById<View>(R.id.demo_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val controller = WindowCompat.getInsetsController(window, root)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }

    override fun onResume() {
        super.onResume()
        // 每次回到前台都重新读一次，片单变动后回来能看到最新数据
        render()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // 预览渲染
    // ------------------------------------------------------------------
    private fun render() {
        renderJob?.cancel()
        renderJob = scope.launch {
            val useSample = sampleSwitch.isChecked
            val data: WidgetData = if (useSample) {
                SampleData.widgetData()
            } else {
                // ContentResolver 查询是阻塞调用，放到 IO 线程
                withContext(Dispatchers.IO) { WidgetRepository.load(applicationContext) }
            }

            statusView.text = buildStatusText(useSample, data)

            // 小号
            val smallViews = WidgetRenderer.small(this@WidgetDemoActivity, data)
            smallPreview.removeAllViews()
            smallPreview.addView(
                smallViews.apply(this@WidgetDemoActivity, smallPreview),
                matchParentParams(),
            )

            // 中号（内部会异步加载海报）
            val mediumViews = WidgetRenderer.medium(this@WidgetDemoActivity, data)
            mediumPreview.removeAllViews()
            mediumPreview.addView(
                mediumViews.apply(this@WidgetDemoActivity, mediumPreview),
                matchParentParams(),
            )
        }
    }

    private fun buildStatusText(useSample: Boolean, data: WidgetData): String = when {
        useSample -> getString(R.string.demo_status_sample)
        data.source == Source.DENIED -> getString(R.string.demo_status_denied)
        data.source == Source.UNAVAILABLE -> getString(R.string.demo_status_unavailable)
        data.hasData -> getString(R.string.demo_status_connected, data.upcomingCount)
        else -> getString(R.string.demo_status_empty)
    }

    private fun matchParentParams() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )

    // ------------------------------------------------------------------
    // 刷新桌面上的组件
    // ------------------------------------------------------------------
    private fun requestWidgetRefresh() {
        val manager = AppWidgetManager.getInstance(this)
        val providers: List<Class<out AppWidgetProvider>> = listOf(
            SmallUpcomingWidgetProvider::class.java,
            MediumUpcomingWidgetProvider::class.java,
        )
        providers.forEach { providerClass ->
            val ids = manager.getAppWidgetIds(ComponentName(this, providerClass))
            if (ids.isEmpty()) return@forEach
            // 显式发一条 APPWIDGET_UPDATE 广播，系统会转成 Provider 的 onUpdate
            val intent = Intent(this, providerClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)
        }
    }

    // ------------------------------------------------------------------
    // 打开主 App
    // ------------------------------------------------------------------
    private fun openMainApp() {
        val intent = packageManager.getLaunchIntentForPackage(WidgetRepository.MAIN_PACKAGE)
        if (intent == null) {
            Toast.makeText(this, R.string.demo_main_app_missing, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(intent)
    }
}