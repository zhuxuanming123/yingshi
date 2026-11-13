package com.b8.movietracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.b8.movietracker.data.local.datastore.settingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataStore 依赖注入模块。
 *
 * 这里只提供「DataStore 实例」这一个底层依赖，
 * 配置项语义（键名、默认值、读写逻辑）由 DataStoreSettingsRepository 负责，
 * 二者职责分离：将来换 proto DataStore 时只改这一处。
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.settingsDataStore
}