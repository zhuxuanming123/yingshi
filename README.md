# B8 影视追踪（B8 Movie Tracker）

基于 TMDB 公开 API 的影视追踪 App：浏览/搜索热门影视、查看详情、维护「想看 / 已看」清单，并伴生桌面小组件（P1 模块）。

---

## 一、技术栈

| 分类 | 技术 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM（ViewModel + 协程 + Flow） |
| 网络 | Retrofit + OkHttp + Gson |
| 图片 | Coil |
| 数据库 | Room |
| 配置存储 | Jetpack DataStore（Preferences） |
| 依赖注入 | Hilt |
| 后台任务 | WorkManager |
| 视频播放 | Media3 (ExoPlayer) |

---

## 二、快速开始

### 1. 打开项目

用 Android Studio 选择 **Open**，打开项目，等待 Gradle 同步（国内网络已配阿里云镜像）。

### 2. 配置 TMDB API Key

1. 到 https://www.themoviedb.org/settings/api 注册并申请 API Key（v3 auth）。
2. 在项目根目录的 `local.properties` 加入：

```properties
TMDB_API_KEY=你的API_Key
```

> `local.properties` 已被 `.gitignore` 忽略，不会提交到 Git。

### 3. 国内网络访问 TMDB

TMDB 服务器在国外，国内网络可能被 DNS 污染。用代理时给模拟器设置：

```bash
adb shell settings put global http_proxy 10.0.2.2:7897
```

### 4. 运行

连接设备/模拟器（API ≥ 24），点击 Run。已实现 5 个页面：首页 / 搜索 / 我的片单 / 详情 / 设置。

---

## 三、目录结构

```
├── gradle/
│   └── libs.versions.toml            # 版本目录
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       └── java/com/b8/movietracker/
│           ├── B8Application.kt      # Hilt 入口 + Coil 图片配置 + 提醒配置同步
│           ├── MainActivity.kt       # Compose 入口（SplashScreen 等配置就绪再渲染）
│           ├── MainViewModel.kt      # 启动期读取主题配置，避免主题闪烁
│           ├── data/
│           │   ├── remote/           # 网络层：TmdbApiService / safeApiCall / MovieMapper / 拦截器 / DTO
│           │   ├── local/            # 本地层：Room（entity / dao / db / Mapper）+ DataStore（配置项）
│           │   └── repository/       # 仓库实现：TmdbMovieRepository / RoomFavoriteRepository / RoomSearchHistoryRepository / DataStoreSettingsRepository
│           ├── domain/
│           │   ├── model/            # 领域模型：Movie / Genre / FilterCriteria / AppSettings
│           │   ├── repository/       # 仓库接口：MovieRepository / FavoriteRepository / SearchHistoryRepository / SettingsRepository
│           │   ├── filter/           # 筛选引擎：FilterEngine
│           │   ├── recommend/        # 推荐引擎：RecommendEngine
│           │   └── statistics/       # 统计模块：StatisticsManager
│           ├── di/                   # Hilt：NetworkModule / RepositoryModule / DatabaseModule / DataStoreModule
│           ├── notification/         # 通知：NotificationHelper（上映提醒）
│           ├── player/               # 播放器：PlayerManager（ExoPlayer 封装）
│           ├── worker/               # 后台任务：WorkManager（定时扫描片单 / 按配置时间调度）
│           ├── ui/
│           │   ├── common/           # UiState + 通用状态视图
│           │   ├── components/       # MovieListItem / PosterImage / VideoPlayer
│           │   ├── home/ search/ mylist/ detail/ settings/   # 5 个页面（detail 含分享卡片预览 ShareCardOverlay）
│           │   ├── navigation/       # NavHost + 路由
│           │   └── theme/
│           └── util/                 # ImageUrlBuilder / DateUtils / ImageCacheManager / ShareCardGenerator / ShareCardStore / GallerySaver
```

---

## 四、架构总览：MVVM + 分层 + 数据源拆分

```
UI 层 (Compose) → ViewModel（StateFlow）→ Repository → 数据源
                                              ├── MovieRepository（网络 TMDB）
                                              └── FavoriteRepository（本地 Room）
```

**核心原则**：
1. 单向数据流，状态只从下往上传。
2. 依赖倒置：ViewModel 只依赖接口，换数据源零改动。
3. 单一职责：网络数据源与本地收藏数据源分离。

---

## 五、三层数据模型

| 模型 | 层 | 作用 |
|---|---|---|
| `MovieDto` | data/remote | 与 TMDB JSON 一一对应 |
| `MovieEntity` | data/local | 与 Room 表结构对应 |
| `Movie` | domain | 面向业务的纯净模型 |

**转换链**：`TMDB JSON → Gson → Dto → MovieMapper → Movie → UI`

---

## 六、统一 UI 状态（UiState）

```kotlin
sealed interface UiState<out T> { Loading / Success<T> / Empty / Error }
```

4 个业务页面统一用它表达状态，配合 `UiStateView` 一键渲染加载/空/错误/成功四种视图。

---

## 七、网络层设计

- **统一异常处理**：`safeApiCall` 捕获 HttpException（404/401/5xx）、IOException（无网络）、其他异常，转成友好提示。
- **线程策略**：Retrofit `suspend` 自动切 IO + `flowOn(Dispatchers.IO)` 双保险；ViewModel 用 `viewModelScope` 防泄漏。
- **图片加载**：Coil 全局配置内存(25%)+磁盘(200MB)缓存，`PosterImage` 组件统一占位/错误容错。

---

## 八、功能模块说明

### 1. 图片缓存与加载（Coil）

- `B8Application` 实现 `ImageLoaderFactory`，全局配置内存(25%)+磁盘(200MB)缓存。
- `PosterImage` 组件统一占位/错误容错，避免加载失败空白崩溃。
- `crossfade` 淡入动画，防止滑动闪烁。

### 2. 后台任务与上映提醒（WorkManager + 通知）

- `ReleaseReminderWorker` 定时扫描片单，上映日期临近时发通知。
- `@HiltWorker` + `HiltWorkerFactory` 实现 Worker 依赖注入。
- `NotificationHelper` 通知渠道 + `PendingIntent` 点击跳详情页。
- 通知权限（Android 13+）申请与拒绝降级处理。
- 开关与时间收敛到「设置」页（DataStore 单一来源），「我的片单」只展示当前状态并可跳转设置，避免两处开关互相矛盾。

### 3. 预告片播放（Media3）

- 对接 TMDB videos 接口，筛选 Trailer 获取 YouTube key。
- ExoPlayer 播放器封装（`PlayerManager` + `VideoPlayer`，含生命周期管控）。
- 因 YouTube 加密流 ExoPlayer 无法播放，改用 Intent 跳转 YouTube 观看。

### 4. 筛选引擎（FilterEngine）

- 纯 Kotlin 模块（domain/filter），与 UI 完全解耦，可独立单元测试。
- `buildQuery()`：把筛选条件（类型/评分/年份/排序）组装成 TMDB discover 接口参数。
- `validate()`：校验条件合法性（评分 0~10、年份起止合法）。

### 5. 搜索历史

- Room 存储（search_history 表），keyword 作主键去重，重复搜索更新时间戳。
- 展示最近 10 条，点击快速搜索，一键清空。

### 6. 观影记录与评分

- movies 表扩展观影字段（用户评分/观影日期/短评/标签/类型）。
- 数据库 2→3 迁移：ALTER TABLE 加可空列，保留老数据。
- 详情页标记「已看」时填写星级评分 + 短评。

### 7. 统计模块（StatisticsManager）

- 纯 Kotlin 模块（domain/statistics），输入已看列表，输出统计结果。
- 统计：已看总数、平均评分、本月观影数、类型分布（groupingBy）。

### 8. 推荐算法（RecommendEngine）

- 纯 Kotlin 标签加权推荐（domain/recommend），与 UI/数据层解耦，可独立单元测试。
- 三步：提取用户类型标签 → 按状态/评分/时间加权 → 匹配候选生成推荐。
- 权重规则：已看 2.0 / 想看 1.0 + 评分÷10 + 近期观影 0.5。
- 首页「为你推荐」横向板块，combine 用户行为与热门自动刷新。
- 行为数据不足时降级展示热门榜单（冷启动处理）。

### 9. 设置中心与配置持久化（Jetpack DataStore）

- 独立「设置」页（底部导航第 4 个 Tab），5 大分区：外观 / 上映提醒 / 默认筛选偏好 / 存储 / 关于。
- `SettingsRepository` 统一收口：接口只暴露 `Flow<AppSettings>` + 若干 `suspend` 写方法；实现 `DataStoreSettingsRepository` 用 Preferences DataStore（`b8_settings`），读走 `data.catch { emit(emptyPreferences()) }.map {}`，写统一 `dataStore.edit {}` 并 `withContext(Dispatchers.IO)`，替代 SharedPreferences。
- 主题模式：跟随系统 / 浅色 / 深色；`Theme.kt` 暴露 `themeMode` 参数，`darkColorScheme()` + `values-night` 资源配合。
- 主题无闪烁：`MainViewModel` 以 `SharingStarted.Eagerly` 先读出配置，`MainActivity` 用 `installSplashScreen()` + `setKeepOnScreenCondition` 把系统启动屏保留到配置就绪，首帧即正确主题。
- 设置 → WorkManager 实时同步：`B8Application` 观察配置流，开关打开按 `reminderHour/Minute` 计算 `initialDelay` 调度周期任务（配置变更 `CANCEL_AND_REENQUEUE`），关闭即取消；Worker 执行前再校验一次开关（双保险）。
- 图片缓存：`ImageCacheManager` 读取 Coil 磁盘缓存大小并支持一键清理（内存 + 磁盘）。

### 10. 首页下拉刷新与分页加载

- 下拉刷新：`PullToRefreshBox`（`ui/components`）封装 material3 1.2.x 的 `PullToRefreshContainer + nestedScrollConnection`（该版本还没有官方的 `PullToRefreshBox`），下拉超过阈值松手触发 `HomeViewModel.refresh()`；刷新期间保留旧列表只转指示器，不闪白、失败也不清空内容。
- 分页加载：`MovieRepository.getPopularMovies(page)` / `discoverMovies(criteria, page)` 透传 `page`；`HomeViewModel` 维护 `currentPage / isLoadingMore / hasMore`，列表滑到倒数第 3 条自动请求下一页并 `distinctBy { id }` 追加；单页不足 20 条判定到底，底部显示「没有更多了」。
- 刷新/分页都按当前筛选条件走（有筛选走 discover，无筛选走热门榜），第一页仍沿用「默认筛选偏好」。

### 11. 影视分享卡片（Canvas 合成 + 系统分享）

- 入口：详情页海报右上角「分享」图标 + 底部「📤 生成分享卡片」按钮，两者都进 `DetailViewModel.generateShareCard()`。
- 合成：`ShareCardGenerator` 用软件 Canvas 画 1080×1440（3:4）竖版卡片 —— 海报由 Coil 加载（命中磁盘缓存）后 `BitmapShader` 居中裁剪 + 28px 圆角 + 半透明描边；标题/简介用 `StaticLayout` 自动换行并限制行数；配色为深色渐变底 + 顶部强调色渐变条 + 品牌水印，右下标注「数据来自 TMDB」。
- 线程：海报加载在 IO、绘制在 `Dispatchers.Default`，全程不占主线程；生成期间 `ShareCardOverlay` 显示 `CircularProgressIndicator` + 「正在生成分享卡片…」，并禁止返回键/点击外部关闭，避免半成品。
- 导出 1（系统分享）：卡片写入 `cacheDir/share/`，经 `FileProvider`（`${applicationId}.fileprovider` + `res/xml/file_paths.xml` 只暴露 `cache/share/`）生成 content:// URI，`ACTION_SEND + FLAG_GRANT_READ_URI_PERMISSION` 拉起系统分享面板 —— Android 10+ 分区存储下**无需任何存储权限**。
- 导出 2（保存相册）：`GallerySaver` 走 MediaStore（`Pictures/B8MovieTracker`，`IS_PENDING` 先占位后置 0）在后台写入；仅 Android 9 及以下才需要 `WRITE_EXTERNAL_STORAGE`（manifest 中已 `maxSdkVersion="28"`）。
- 权限降级：存储权限被拒时不阻断流程 —— Toast 提示「仍可通过「分享」发送卡片」，分享入口始终可用。
- 顺带修掉的稳定性问题：首页推荐流是网络请求，断网/服务异常时异常会冲出 `stateIn` 把 ViewModel 协程打挂（表现为启动即闪退），已在 `recommendedMovies` 链路加 `catch { emit(emptyList()) }` 兜底。

---

## 九、API Key 安全管理

```
local.properties（.gitignore 忽略）
  → buildConfigField → BuildConfig → ApiConfig → AuthInterceptor（附加 api_key）
```

密钥不进源码、不进 Git；换环境只改 `local.properties`。

---

## 十、迭代路线

- ✅ 阶段0：项目初始化（骨架 + 依赖 + 数据模型 + 网络封装）
- ✅ UI 层：四页面 + 导航 + ViewModel + 统一状态
- ✅ 网络对接 TMDB（safeApiCall / Mapper / Repository 拆分）
- ✅ Room 本地持久化（收藏写库，重启不丢）
- ✅ 图片缓存与加载优化
- ✅ 后台任务与上映提醒（阶段5：WorkManager + 通知）
- ✅ 预告片播放（Media3 + TMDB 视频接口，跳转 YouTube 观看）
- ✅ 高级筛选与搜索历史（FilterEngine + 类型/评分/年份/排序 + Room 历史）
- ✅ 观影记录与个人评分（评分/短评 + 想看/已看 Tab + 统计模块）
- ✅ 本地个性化推荐（RecommendEngine 标签加权算法）
- ✅ 设置中心与配置持久化（Jetpack DataStore：主题模式 / 提醒开关与时间 / 默认筛选 / 缓存清理）
- ✅ 首页下拉刷新 + 列表分页加载（滑到底部自动加载下一页）
- ✅ 影视分享卡片（Canvas 合成卡片 + 系统分享 / 保存相册，FileProvider + MediaStore）
- ⬜ P1 桌面小组件（Glance + WorkManager）
- ⬜ 打磨（错误重试 / 性能 / 打包）
