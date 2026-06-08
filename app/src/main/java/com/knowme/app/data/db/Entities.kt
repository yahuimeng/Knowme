package com.knowme.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一条通知的优先级，由 AI 消化后回填；未分析时为 UNKNOWN。 */
enum class Priority { HIGH, MID, LOW, UNKNOWN }

/**
 * 一条原始通知。命根子数据。
 * 注意：原文按隐私策略定期清理，详见 NotificationDao.deleteOlderThan。
 */
@Entity(
    tableName = "notifications",
    indices = [Index("postedAt"), Index("packageName"), Index("sbnKey")],
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sbnKey: String,            // StatusBarNotification.key，用于去重
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAt: Long,            // epoch millis
    val priority: Priority = Priority.UNKNOWN,
    val summary: String? = null,   // AI 提炼的一句话摘要
    val handled: Boolean = false,  // 用户是否已处理/已读
)

/** 从通知中抽取出的待办。 */
@Entity(
    tableName = "todos",
    indices = [Index("createdAt"), Index("done")],
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val sourceNotificationId: Long? = null,
    val sourceLabel: String? = null,   // 例如 "微信·王总 09:12"
    val createdAt: Long,
    val done: Boolean = false,
    val doneAt: Long? = null,
)

/** 一次 AI 调用的 token 用量记录。 */
@Entity(tableName = "token_usage", indices = [Index("createdAt")])
data class TokenUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val kind: String,          // digest / ask / chat
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
)

/** 「问问」的一问一答历史记录。 */
@Entity(tableName = "asks", indices = [Index("createdAt")])
data class AskMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val answer: String,
    val isError: Boolean = false,
    val createdAt: Long,
)

/**
 * 每日早报（AI 消化结果）。一天一条，便于"今日"页直接读取。
 */
@Entity(tableName = "digests")
data class DigestEntity(
    @PrimaryKey val dateKey: String,   // yyyy-MM-dd（本地时区）
    val narrative: String,             // 顶部那段"今天发生了什么"
    val notificationCount: Int,
    val noiseFolded: Int,
    val generatedAt: Long,
)
