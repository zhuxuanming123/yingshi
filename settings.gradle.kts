// ============================================================
// settings.gradle.kts —— 项目顶层设置
// 作用：声明依赖仓库、根项目名称、以及包含哪些子模块
//
// 说明：国内网络环境已将仓库替换为阿里云镜像，下载依赖更快；
//       官方源(google/mavenCentral)保留在末尾作为兜底。
// ============================================================

pluginManagement {
    repositories {
        // ===== 阿里云镜像（国内加速，优先级高）=====
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        // ===== 官方源（兜底）=====
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 强制所有依赖只从下方声明的仓库解析，禁止子模块私自加仓库（更规范）
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 阿里云镜像（国内加速，优先级高）=====
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/public")
        // ===== 官方源（兜底）=====
        google()
        mavenCentral()
    }
}

// 根项目名
rootProject.name = "B8MovieTracker"

// 包含的模块：主 App
include(":app")
// 阶段7 再引入桌面小组件伴生模块 P1
// include(":widget:p1")
