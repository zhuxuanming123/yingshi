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
| 依赖注入 | Hilt |
| 后台任务 | WorkManager |

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

连接设备/模拟器（API ≥ 24），点击 Run。已实现 4 个页面：首页 / 搜索 / 我的片单 / 详情。

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
│           ├── B8Application.kt      # Hilt 入口 + Coil 图片配置
│           ├── MainActivity.kt       # Compose 入口
│           ├── data/
│           │   ├── remote/           # 网络层：TmdbApiService / safeApiCall / MovieMapper / 拦截器 / DTO
│           │   ├── local/            # 本地层：Room（entity / dao / db / Mapper）
│           │   └── repository/       # 仓库实现：TmdbMovieRepository / RoomFavoriteRepository
│           ├── domain/
│           │   ├── model/            # 领域模型：Movie
│           │   └── repository/       # 仓库接口：MovieRepository / FavoriteRepository
│           ├── di/                   # Hilt：NetworkModule / RepositoryModule / DatabaseModule
│           ├── ui/
│           │   ├── common/           # UiState + 通用状态视图
│           │   ├── components/       # MovieListItem / PosterImage
│           │   ├── home/ search/ mylist/ detail/   # 4 个页面
│           │   ├── navigation/       # NavHost + 路由
│           │   └── theme/
│           └── util/                 # ImageUrlBuilder
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

4 个页面统一用它表达状态，配合 `UiStateView` 一键渲染加载/空/错误/成功四种视图。

---

## 七、网络层设计

- **统一异常处理**：`safeApiCall` 捕获 HttpException（404/401/5xx）、IOException（无网络）、其他异常，转成友好提示。
- **线程策略**：Retrofit `suspend` 自动切 IO + `flowOn(Dispatchers.IO)` 双保险；ViewModel 用 `viewModelScope` 防泄漏。
- **图片加载**：Coil 全局配置内存(25%)+磁盘(200MB)缓存，`PosterImage` 组件统一占位/错误容错。

---

## 八、API Key 安全管理

```
local.properties（.gitignore 忽略）
  → buildConfigField → BuildConfig → ApiConfig → AuthInterceptor（附加 api_key）
```

密钥不进源码、不进 Git；换环境只改 `local.properties`。

---

## 九、迭代路线

- ✅ 阶段0：项目初始化（骨架 + 依赖 + 数据模型 + 网络封装）
- ✅ UI 层：四页面 + 导航 + ViewModel + 统一状态
- ✅ 网络对接 TMDB（safeApiCall / Mapper / Repository 拆分）
- ✅ Room 本地持久化（收藏写库，重启不丢）
- ✅ 图片缓存与加载优化
- ⬜ 列表分页加载
- ⬜ P1 桌面小组件（Glance + WorkManager）
- ⬜ 打磨（深色模式 / 错误重试 / 性能 / 打包）
