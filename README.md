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
│           ├── data/          # 数据层（remote 网络 / local 数据库）
│           ├── domain/        # 领域模型
│           ├── di/            # 依赖注入
│           └── util/          # 工具
```

## 迭代路线

- ✅ 阶段0：项目初始化（骨架 + 依赖 + 数据模型 + 网络封装）
- ⬜ UI 层：四页面 + 导航 + ViewModel
- ⬜ 网络对接 TMDB
- ⬜ Room 本地持久化
- ⬜ 图片缓存优化
- ⬜ 列表分页
- ⬜ P1 桌面小组件
- ⬜ 打磨
