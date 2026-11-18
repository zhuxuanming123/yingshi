// ============================================================
// widget/p1/build.gradle.kts —— 桌面小组件伴生模块（P1）
//
// 这是一个「独立 APK」模块，不是 library：
//   - 可以单独 `installDebug` 到桌面演示（任务要求：独立可运行、可演示）；
//   - 通过主 App 暴露的只读 ContentProvider 读取片单数据，
//     主 App 未安装时自动降级为空态，不影响组件添加。
// ============================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.b8.movietracker.widget.p1"
    compileSdk = 35

    defaultConfig {
        // 独立包名：与主 App（com.b8.movietracker）区分开，
        // 两个 APK 可同时安装，小组件互不干扰
        applicationId = "com.b8.movietracker.widget.p1"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 仅保留最小依赖：小组件用 RemoteViews（原生 View），不需要 Compose / Hilt / Room
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}