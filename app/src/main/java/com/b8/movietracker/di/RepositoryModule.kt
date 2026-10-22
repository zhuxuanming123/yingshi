package com.b8.movietracker.di

import com.b8.movietracker.data.local.dao.MovieDao
import com.b8.movietracker.data.remote.TmdbApiService
import com.b8.movietracker.data.repository.RoomFavoriteRepository
import com.b8.movietracker.data.repository.TmdbMovieRepository
import com.b8.movietracker.domain.repository.FavoriteRepository
import com.b8.movietracker.domain.repository.MovieRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据仓库依赖注入模块。
 *
 * 依赖倒置的体现：ViewModel 只依赖接口，这里决定提供哪个实现。
 * 阶段3：FavoriteRepository 换成 Room 实现（持久化），ViewModel/UI 零改动。
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /** 网络数据源：TMDB 实现 */
    @Provides
    @Singleton
    fun provideMovieRepository(apiService: TmdbApiService): MovieRepository =
        TmdbMovieRepository(apiService)

    /** 本地收藏：Room 持久化实现（替换阶段2 的内存实现） */
    @Provides
    @Singleton
    fun provideFavoriteRepository(movieDao: MovieDao): FavoriteRepository =
        RoomFavoriteRepository(movieDao)
}
