package com.b8.movietracker.data.remote

import com.b8.movietracker.data.remote.dto.MovieDetailDto
import com.b8.movietracker.data.remote.dto.MovieListResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB API 接口定义（任务6：Retrofit Service 模板）。
 *
 * Retrofit 原理简述：
 *   这是「接口」，没有实现类。Retrofit 在运行期用动态代理生成实现，
 *   根据注解拼装 URL、解析参数、并把返回的 JSON 交给 Gson 转成 DTO。
 *
 * 关于 suspend：
 *   方法声明为 suspend 后，Retrofit 内部会自动切换到 IO 线程执行网络请求，
 *   调用方无需手动 withContext(Dispatchers.IO)。这正是"不在主线程做网络"的关键。
 */
interface TmdbApiService {

    /** 获取热门影片列表（分页） */
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = ApiConfig.DEFAULT_LANGUAGE,
        @Query("region") region: String = ApiConfig.DEFAULT_REGION,
    ): MovieListResponseDto

    /** 获取评分最高的影片列表（分页） */
    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = ApiConfig.DEFAULT_LANGUAGE,
        @Query("region") region: String = ApiConfig.DEFAULT_REGION,
    ): MovieListResponseDto

    /** 获取本周趋势影片（分页） */
    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1,
        @Query("language") language: String = ApiConfig.DEFAULT_LANGUAGE,
    ): MovieListResponseDto

    /** 搜索电影（按关键词，任务1） */
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = ApiConfig.DEFAULT_LANGUAGE,
    ): MovieListResponseDto

    /**
     * 获取影片详情。
     * @param movieId 影片 ID（路径参数，用 @Path 注入）
     * @param appendToResponse 追加返回演员、预告片等数据，减少请求次数
     */
    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Long,
        @Query("language") language: String = ApiConfig.DEFAULT_LANGUAGE,
        @Query("append_to_response") appendToResponse: String = "credits,videos",
    ): MovieDetailDto
}
