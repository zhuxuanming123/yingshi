package com.b8.movietracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.b8.movietracker.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

/**
 * 影片 DAO（数据访问对象，任务2）。
 *
 * Room 会在编译期根据这里的 SQL 和注解生成实现类。
 * 返回 Flow 的查询会自动「观察」表变化：数据一改，Flow 自动发出新值，
 * 这是「我的片单」实时刷新的基石。
 */
@Dao
interface MovieDao {

    /** 新增收藏（任务2）。主键冲突时替换，实现"重复收藏时更新" */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movie: MovieEntity)

    /** 删除收藏（任务2，按 id 删除） */
    @Query("DELETE FROM movies WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 按 id 查询影片（任务2，判断是否已收藏） */
    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun getMovieById(id: Long): MovieEntity?

    /** 查询全部片单（任务2），按加入时间倒序 */
    @Query("SELECT * FROM movies ORDER BY addedAt DESC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    /** 按片单类型查询（想看/已看），按加入时间倒序 */
    @Query("SELECT * FROM movies WHERE watchStatus = :status ORDER BY addedAt DESC")
    fun getMoviesByStatus(status: Int): Flow<List<MovieEntity>>
}
