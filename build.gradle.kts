// ============================================================
// build.gradle.kts —— 根构建脚本
// 作用：声明所有子模块会用到的 Gradle 插件
// 注意：这里用 apply false，表示"仅声明版本、暂不应用"，
//       真正的应用发生在各子模块（如 app/build.gradle.kts）里
// ============================================================

plugins {
    // Android 应用 / 库插件
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // Kotlin 插件
    alias(libs.plugins.kotlin.android) apply false
    // Kotlin 符号处理（KSP），用于 Hilt / Room 的注解处理器
    alias(libs.plugins.ksp) apply false
    // Hilt 依赖注入插件
    alias(libs.plugins.hilt) apply false
}
