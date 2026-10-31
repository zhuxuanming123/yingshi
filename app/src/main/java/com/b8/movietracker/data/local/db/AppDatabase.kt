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
 * 版本历史：
 *   - version 1：movies 表（基础片单）
 *   - version 2：+ search_history 表（搜索历史）
 *   - version 3：movies 表 + 观影记录字段（评分/日期/短评/标签/类型）
 */
@Database(
    entities = [MovieEntity::class, SearchHistoryEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        /** 迁移 1 -> 2：新增 search_history 表 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS search_history (" +
                        "keyword TEXT PRIMARY KEY NOT NULL, " +
                        "timestamp INTEGER NOT NULL)"
                )
            }
        }

        /** 迁移 2 -> 3：movies 表新增观影记录字段 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movies ADD COLUMN userRating REAL")
                db.execSQL("ALTER TABLE movies ADD COLUMN watchedDate INTEGER")
                db.execSQL("ALTER TABLE movies ADD COLUMN review TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN tags TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN genreIds TEXT")
            }
        }
    }
}
