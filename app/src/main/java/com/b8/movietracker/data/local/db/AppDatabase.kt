package com.b8.movietracker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.b8.movietracker.data.local.dao.MovieDao
import com.b8.movietracker.data.local.entity.MovieEntity

/**
 * Room 数据库定义（任务5）。
 *
 * @Database 声明数据库包含哪些表（entities）以及版本号。
 *   - version：数据库结构变更时需 +1，并配合 Migration 迁移（阶段8 讲解）
 *   - exportSchema：是否导出 schema 文件；发布项目建议设为 true 以便迁移
 *
 * 注意：这里只定义抽象类，真正的数据库实例在阶段2 由 Hilt 模块创建并注入。
 */
@Database(
    entities = [MovieEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    /** 暴露 MovieDao，供 Repository 使用 */
    abstract fun movieDao(): MovieDao
}
