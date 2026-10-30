package com.b8.movietracker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.b8.movietracker.data.local.dao.MovieDao
import com.b8.movietracker.data.local.dao.SearchHistoryDao
import com.b8.movietracker.data.local.entity.MovieEntity
import com.b8.movietracker.data.local.entity.SearchHistoryEntity

/**
 * Room 数据库定义。
 *
 * @Database 声明数据库包含哪些表（entities）以及版本号。
 * 阶段升级：version 1 -> 2，新增 search_history 表（搜索历史）。
 */
@Database(
    entities = [MovieEntity::class, SearchHistoryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        /**
         * 数据库迁移：1 -> 2，新增 search_history 表。
         * 迁移的意义：老用户升级 App 时，保留原有收藏数据，只新增表。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS search_history (" +
                        "keyword TEXT PRIMARY KEY NOT NULL, " +
                        "timestamp INTEGER NOT NULL)"
                )
            }
        }
    }
}
