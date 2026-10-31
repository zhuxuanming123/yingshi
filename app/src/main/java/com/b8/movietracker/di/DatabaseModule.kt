package com.b8.movietracker.di

import android.content.Context
import androidx.room.Room
import com.b8.movietracker.data.local.dao.MovieDao
import com.b8.movietracker.data.local.dao.SearchHistoryDao
import com.b8.movietracker.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room 数据库依赖注入模块。
 *
 * 关键点（答辩常考）：
 *   - @ApplicationContext 提供应用级 Context（而非 Activity Context），
 *     避免数据库实例随 Activity 生命周期被反复创建/销毁导致泄漏。
 *   - @Singleton 保证整个应用只有一份数据库实例。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** 提供 Room 数据库单例 */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "b8_movie.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    /** 提供 DAO（由数据库实例派生） */
    @Provides
    fun provideMovieDao(database: AppDatabase): MovieDao = database.movieDao()

    @Provides
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao =
        database.searchHistoryDao()
}
