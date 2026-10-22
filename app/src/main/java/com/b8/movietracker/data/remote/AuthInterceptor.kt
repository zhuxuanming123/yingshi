package com.b8.movietracker.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 鉴权拦截器：给每个请求自动附加 api_key 参数。
 *
 * TMDB 的公开 API 采用「URL query 参数 api_key=xxx」方式鉴权，
 * 而不是常见的 HTTP Header（Authorization）。因此这里重写 URL。
 *
 * 为什么用拦截器而不是在每个接口手写 @Query("api_key")？
 *   1. 单一职责：密钥只在这一个地方出现，避免散落各处
 *   2. 不易遗漏：所有请求自动带上
 *   3. 便于后续切换鉴权方式（如换成 v4 的 Bearer Token，只需改这里）
 */
class AuthInterceptor(
    private val apiKey: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        // 基于原 URL 追加 query 参数，保留原有其它参数
        val newUrl = originalRequest.url.newBuilder()
            .addQueryParameter("api_key", apiKey)
            .build()
        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()
        // proceed：把请求交给拦截器链的下一环（最终发到服务器）
        return chain.proceed(newRequest)
    }
}
