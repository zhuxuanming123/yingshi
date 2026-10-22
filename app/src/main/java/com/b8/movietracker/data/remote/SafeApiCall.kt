package com.b8.movietracker.data.remote

import retrofit2.HttpException
import java.io.IOException

/**
 * 网络层统一异常（任务2：统一异常捕获）。
 * 所有网络错误都会被 safeApiCall 转成 ApiException，携带友好中文提示。
 *
 * @param code HTTP 状态码（如 404、401），非 HTTP 错误时为 null
 */
class ApiException(message: String, val code: Int? = null) : Exception(message)

/**
 * 网络请求安全包装器（任务2 + 约束3：协程处理、不阻塞主线程）。
 *
 * 作用：统一捕获三类异常，转成用户能看懂的 ApiException：
 *   1. HttpException —— HTTP 层错误（404 资源不存在、401 API Key 无效、5xx 服务器错误）
 *   2. IOException  —— 网络层错误（无连接、超时、DNS 解析失败）
 *   3. 其它异常      —— 反序列化失败等意外情况
 *
 * 关于线程：
 *   Retrofit 的 suspend 方法内部已自动切到 IO 线程执行网络请求；
 *   调用方在 Repository 里再叠加 flowOn(Dispatchers.IO)，双重保证不占主线程。
 */
suspend fun <T> safeApiCall(block: suspend () -> T): T {
    return try {
        block()
    } catch (e: HttpException) {
        throw ApiException(handleHttpError(e), e.code())
    } catch (e: IOException) {
        throw ApiException("网络连接失败，请检查网络后重试")
    } catch (e: Exception) {
        throw ApiException(e.message ?: "请求失败，请稍后重试")
    }
}

/** 把 HTTP 状态码翻译成友好提示（任务2：token 错误、404 等） */
private fun handleHttpError(e: HttpException): String {
    return when (e.code()) {
        401 -> "API Key 无效或已过期，请检查 local.properties 配置"
        403 -> "无权访问该资源"
        404 -> "请求的资源不存在"
        429 -> "请求过于频繁，请稍后重试"
        in 500..599 -> "服务器开小差了，请稍后重试"
        else -> "请求失败（HTTP ${e.code()}）"
    }
}
