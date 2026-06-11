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
    val sender: String? = null,    // 发信人/来源（IM 场景常为 title），用于"越用越懂你"按来源学习
    val category: String? = null,  // 系统通知分类（msg/email/...），辅助信号
)

/**
 * 「越用越懂你」的偏好信号聚合：按"来源"累计用户的互动（点开/展开=engaged，划走=ignored）。
 * 纯本地、零配置：从日常行为里被动学习，不让用户填表。
 */
@Entity(tableName = "pref_signals")
data class PrefSignalEntity(
    @PrimaryKey val key: String,   // "app:<pkg>" 或 "src:<pkg>|<sender>"
    val kind: String,              // "APP" / "SENDER"
    val packageName: String,
    val label: String,             // 显示名，如「微信·老张」或「微信」
    val engaged: Int = 0,          // 在乎信号累计（点开/展开）
    val ignored: Int = 0,          // 不在乎信号累计（划走）
    val updatedAt: Long = 0,
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

/** 「问问」的一问一答历史记录（旧版，保留兼容）。 */
@Entity(tableName = "asks", indices = [Index("createdAt")])
data class AskMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val answer: String,
    val isError: Boolean = false,
    val createdAt: Long,
)

/** 一个对话会话。mode：CHAT=自由聊天，NOTIFICATION=问通知（答案只依据本地通知）。 */
@Entity(tableName = "conversations", indices = [Index("updatedAt")])
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val mode: String,        // "CHAT" / "NOTIFICATION"
    val createdAt: Long,
    val updatedAt: Long,
)

/** 对话里的一条消息。role：user / assistant。 */
@Entity(tableName = "messages", indices = [Index("conversationId"), Index("createdAt")])
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,        // "user" / "assistant"
    val content: String,
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
