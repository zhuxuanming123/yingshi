// ============================================================
// app/build.gradle.kts —— 主 App 模块构建脚本
// 包含：插件、Android 配置、依赖、以及 TMDB API Key 的安全注入
// ============================================================

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ============================================================
// 一、TMDB API Key 的安全注入
// 目的：API Key 属于敏感信息，绝不硬编码进源码、也不提交 Git
// 做法：从 local.properties 读取 -> 注入到 BuildConfig 常量
// ============================================================
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
// 读取不到时给空串（此时仍可编译，运行时会提示"未配置"）
val tmdbApiKey: String = localProperties.getProperty("TMDB_API_KEY") ?: ""

android {
    namespace = "com.b8.movietracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.b8.movietracker"
        minSdk = 24                 // 任务要求：最小支持 Android 7.0
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // 把 API Key / 接口地址注入 BuildConfig，代码里通过 BuildConfig.XXX 读取
        // 注意：字符串值必须再用一对双引号包裹，否则会被当成变量名导致编译失败
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "TMDB_BASE_URL", "\"https://api.themoviedb.org/3/\"")
        buildConfigField("String", "TMDB_IMAGE_BASE_URL", "\"https://image.tmdb.org/t/p/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Java 与 Kotlin 编译目标统一为 17（AGP 8.x 要求）
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true        // 启用 Jetpack Compose
        buildConfig = true    // 启用 BuildConfig 生成（用于 API Key 注入）
    }
    composeOptions {
        // Compose 编译器版本，必须与 Kotlin 版本配套（1.9.24 -> 1.5.14）
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ===================== AndroidX 基础 =====================
    implementation(libs.androidx.core.ktx)                 // 含 NotificationCompat（通知相关）
    implementation(libs.androidx.lifecycle.runtime.ktx)    // Lifecycle 基础
    implementation(libs.androidx.lifecycle.viewmodel.ktx)  // ViewModel + viewModelScope
    implementation(libs.androidx.activity.compose)

    // ===================== Jetpack Compose =====================
    implementation(platform(libs.androidx.compose.bom))    // BOM 统一 Compose 各库版本
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose) // collectAsStateWithLifecycle

    // ===================== Hilt 依赖注入 =====================
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)                                // Hilt 注解处理器
    implementation(libs.androidx.hilt.navigation.compose)  // Compose 中获取 HiltViewModel
    implementation(libs.androidx.hilt.work)               // Hilt 集成 WorkManager

    // ===================== 网络 =====================
    implementation(libs.retrofit)                          // Retrofit 核心
    implementation(libs.retrofit.converter.gson)           // Gson 转换器
    implementation(libs.okhttp)                            // OkHttp
    implementation(libs.okhttp.logging)                    // 日志拦截器（调试用）
    implementation(libs.gson)                              // Gson JSON 解析

    // ===================== 图片加载/缓存 =====================
    implementation(libs.coil.compose)                      // Coil：协程友好的图片加载

    // ===================== 数据库 =====================
    implementation(libs.room.runtime)                      // Room
    implementation(libs.room.ktx)                          // Room 协程扩展
    ksp(libs.room.compiler)                                // Room 注解处理器

    // ===================== 协程 =====================
    implementation(libs.kotlinx.coroutines.android)

    // ===================== 后台任务 =====================
    implementation(libs.androidx.work.runtime.ktx)         // WorkManager（小组件定时刷新等）

    // ===================== 测试 =====================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
