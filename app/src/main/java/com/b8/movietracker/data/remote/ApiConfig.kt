package com.b8.movietracker.data.remote

import com.b8.movietracker.BuildConfig

/**
 * TMDB API 基础配置（任务6：API Key 管理）。
 *
 * 关键设计 —— API Key 的安全管理链路：
 *   local.properties（本地，不入库）
 *        │  app/build.gradle.kts 读取
 *        ▼
 *   BuildConfig.TMDB_API_KEY（编译期注入的常量）
 *        │  本类读取
 *        ▼
 *   ApiConfig.API_KEY（代码统一入口）
 *        │  AuthInterceptor 使用
 *        ▼
 *   附加到每个请求的 query 参数
 *
 * 好处：密钥不进源码、不进 Git；换环境只需改 local.properties。
 */
object ApiConfig {

    /** 接口基础地址，来自 BuildConfig */
    const val BASE_URL: String = BuildConfig.TMDB_BASE_URL

    /** 图片基础地址，来自 BuildConfig */
    const val IMAGE_BASE_URL: String = BuildConfig.TMDB_IMAGE_BASE_URL

    /** TMDB API Key，来自 BuildConfig（源头是 local.properties） */
    const val API_KEY: String = BuildConfig.TMDB_API_KEY

    /** 默认返回语言：中文 */
    const val DEFAULT_LANGUAGE: String = "zh-CN"

    /** 默认地区：中国 */
    const val DEFAULT_REGION: String = "CN"

    /**
     * 判断 API Key 是否已正确配置。
     * 未配置时仍可编译，但请求会失败，UI 层可据此提示用户。
     */
    fun isApiKeyConfigured(): Boolean = API_KEY.isNotBlank()

    /** TMDB 支持的图片尺寸后缀（拼接完整海报 URL 用） */
    object ImageSize {
        const val POSTER = "w342"    // 列表海报（中等尺寸）
        const val BACKDROP = "w780"  // 详情页背景大图
        const val PROFILE = "w185"   // 头像
    }
}
