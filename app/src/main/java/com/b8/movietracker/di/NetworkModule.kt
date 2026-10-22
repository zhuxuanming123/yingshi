package com.b8.movietracker.di

import com.b8.movietracker.BuildConfig
import com.b8.movietracker.data.remote.ApiConfig
import com.b8.movietracker.data.remote.AuthInterceptor
import com.b8.movietracker.data.remote.TmdbApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块：负责提供网络层相关对象。
 *
 * 几个关键注解：
 *   @Module       —— 标记这是一个提供依赖的模块
 *   @InstallIn(SingletonComponent::class) —— 这些依赖挂载在「应用级单例」容器里，
 *                    即整个应用生命周期内只有一份实例
 *   @Provides     —— 标记方法用于构造并暴露某个依赖
 *   @Singleton    —— 该方法产出的对象是单例（与 SingletonComponent 作用域一致）
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 提供 Gson 实例。
     * setLenient()：放宽解析，容忍一些不规范 JSON（如数字被写成字符串）。
     */
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    /** 提供鉴权拦截器，注入 API Key */
    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor = AuthInterceptor(ApiConfig.API_KEY)

    /**
     * 提供 OkHttpClient。
     * 挂载：
     *   1. AuthInterceptor —— 自动附加 api_key
     *   2. HttpLoggingInterceptor —— 仅 Debug 下打印请求日志（生产环境关闭，避免泄露/性能损耗）
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)  // 连接超时
            .readTimeout(15, TimeUnit.SECONDS)     // 读取超时
            .addInterceptor(authInterceptor)

        // 仅在 Debug 构建打印网络日志
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }
        return builder.build()
    }

    /**
     * 提供 Retrofit 实例。
     * 绑定 baseUrl、OkHttpClient、以及 Gson 转换器。
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)                    // 必须以 / 结尾
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))  // JSON -> DTO
            .build()

    /** 提供 TMDB API 接口的动态代理实现 */
    @Provides
    @Singleton
    fun provideTmdbApiService(retrofit: Retrofit): TmdbApiService =
        retrofit.create(TmdbApiService::class.java)
}
