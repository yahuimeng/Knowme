package com.knowme.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: NotificationEntity): Long

    @Update
    suspend fun update(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY postedAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE postedAt >= :since ORDER BY postedAt DESC")
    suspend fun since(since: Long): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE postedAt BETWEEN :start AND :end ORDER BY postedAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeCount(): Flow<Int>

    /** 隐私：清理早于某时间点的原文。 */
    @Query("DELETE FROM notifications WHERE postedAt < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM notifications")
    suspend fun clear()
}

@Dao
interface TodoDao {
    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("SELECT * FROM todos ORDER BY done ASC, createdAt DESC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("SELECT COUNT(*) FROM todos WHERE done = 0")
    fun observeOpenCount(): Flow<Int>

    @Query("DELETE FROM todos")
    suspend fun clear()
}

@Dao
interface DigestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(digest: DigestEntity)

    @Query("SELECT * FROM digests WHERE dateKey = :dateKey")
    fun observe(dateKey: String): Flow<DigestEntity?>

    @Query("DELETE FROM digests")
    suspend fun clear()
}
