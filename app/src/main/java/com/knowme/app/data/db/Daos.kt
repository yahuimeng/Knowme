package com.knowme.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 出现过通知的 App 及其条数（用于「监听哪些 App」列表）。 */
data class AppNotifCount(
    val packageName: String,
    val appName: String,
    val count: Int,
)

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

    @Query("SELECT * FROM notifications WHERE postedAt BETWEEN :start AND :end ORDER BY postedAt ASC")
    suspend fun between(start: Long, end: Long): List<NotificationEntity>

    /** 观察某天通知（供「今日」三档分组）。 */
    @Query("SELECT * FROM notifications WHERE postedAt BETWEEN :start AND :end ORDER BY postedAt DESC")
    fun observeDay(start: Long, end: Long): Flow<List<NotificationEntity>>

    /** AI 消化后回填优先级与摘要。 */
    @Query("UPDATE notifications SET priority = :priority, summary = :summary WHERE id = :id")
    suspend fun setAnalysis(id: Long, priority: Priority, summary: String?)

    @Query("SELECT * FROM notifications WHERE postedAt BETWEEN :start AND :end ORDER BY postedAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeCount(): Flow<Int>

    /** 自某时间点之后新到的通知数（用于"无新通知则不消化"）。 */
    @Query("SELECT COUNT(*) FROM notifications WHERE postedAt > :since")
    suspend fun countSince(since: Long): Int

    @Query(
        "SELECT packageName AS packageName, MAX(appName) AS appName, COUNT(*) AS count " +
            "FROM notifications GROUP BY packageName ORDER BY count DESC"
    )
    fun observeApps(): Flow<List<AppNotifCount>>

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

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("SELECT * FROM todos ORDER BY done ASC, createdAt DESC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("SELECT COUNT(*) FROM todos WHERE done = 0")
    fun observeOpenCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE content = :content")
    suspend fun countWithContent(content: String): Int

    /**
     * 重新生成早报前，清掉当天未完成的待办，避免重复累积。
     * 不再限制 sourceNotificationId（AI 抽的待办常无来源 id），否则永远删不掉会叠加。
     * 已完成的保留（由 countWithContent 去重，不会被重新加回）。
     */
    @Query("DELETE FROM todos WHERE done = 0 AND createdAt >= :since")
    suspend fun deleteAutoUndoneSince(since: Long)

    @Query("DELETE FROM todos")
    suspend fun clear()
}

/** token 用量汇总。 */
data class TokenTotals(
    val input: Long,
    val output: Long,
    val calls: Int,
)

/** 某天的 token 用量（input+output 合计）。 */
data class DailyTokens(
    val day: String,   // yyyy-MM-dd（本地时区）
    val total: Long,
)

@Dao
interface TokenUsageDao {
    @Insert
    suspend fun insert(usage: TokenUsageEntity): Long

    @Query(
        "SELECT strftime('%Y-%m-%d', createdAt/1000, 'unixepoch', 'localtime') AS day, " +
            "SUM(inputTokens + outputTokens) AS total FROM token_usage " +
            "WHERE createdAt >= :since GROUP BY day"
    )
    fun observeDaily(since: Long): Flow<List<DailyTokens>>

    @Query(
        "SELECT COALESCE(SUM(inputTokens),0) AS input, COALESCE(SUM(outputTokens),0) AS output, " +
            "COUNT(*) AS calls FROM token_usage"
    )
    fun observeTotals(): Flow<TokenTotals>

    @Query(
        "SELECT COALESCE(SUM(inputTokens),0) AS input, COALESCE(SUM(outputTokens),0) AS output, " +
            "COUNT(*) AS calls FROM token_usage WHERE createdAt >= :since"
    )
    fun observeTotalsSince(since: Long): Flow<TokenTotals>

    @Query("DELETE FROM token_usage")
    suspend fun clear()
}

@Dao
interface AskDao {
    @Insert
    suspend fun insert(message: AskMessageEntity): Long

    @Query("SELECT * FROM asks ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<AskMessageEntity>>

    @Query("DELETE FROM asks")
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
