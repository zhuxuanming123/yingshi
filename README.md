# B8 影视追踪（B8 Movie Tracker）

基于 TMDB 公开 API 的影视追踪 App：浏览/搜索热门影视、查看详情、维护「想看 / 已看」清单，并伴生桌面小组件（P1 模块）。

## 技术栈

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

## 快速开始

1. 用 Android Studio 打开项目，等待 Gradle 同步。
2. 在项目根目录的 `local.properties` 配置 TMDB API Key（见 `local.properties.example`）。
3. 连接设备（API ≥ 24）运行。

## 目录结构

```
├── gradle/            # 版本目录
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/b8/movietracker/
│           ├── data/
│           │   ├── remote/       # 网络层：TmdbApiService / safeApiCall / MovieMapper / DTO
│           │   ├── local/        # 本地层：Room（entity / dao / db / Mapper）
│           │   └── repository/   # 仓库实现：TmdbMovieRepository / RoomFavoriteRepository
│           ├── domain/
│           │   ├── model/        # 领域模型：Movie
│           │   └── repository/   # 仓库接口：MovieRepository / FavoriteRepository
│           ├── di/               # Hilt：NetworkModule / RepositoryModule / DatabaseModule
│           └── util/             # ImageUrlBuilder
```

## 架构说明

- **MVVM 分层**：UI → ViewModel → Repository → 数据源，单向数据流。
- **数据源拆分**：网络（MovieRepository）与本地收藏（FavoriteRepository）分离。
- **三层数据模型**：Dto（网络）/ Entity（数据库）/ Movie（业务）解耦。
- **API Key 安全**：`local.properties` → BuildConfig → 拦截器，密钥不入库。

## 迭代路线

- ✅ 阶段0：项目初始化（骨架 + 依赖 + 数据模型 + 网络封装）
- ⬜ UI 层：四页面 + 导航 + ViewModel
- ✅ 网络对接 TMDB（safeApiCall / MovieMapper / Repository 拆分）
- ✅ Room 本地持久化（收藏写库，重启不丢）
- ⬜ 图片缓存优化
- ⬜ 列表分页
- ⬜ P1 桌面小组件
- ⬜ 打磨
