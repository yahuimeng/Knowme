package com.knowme.app.digest

import com.knowme.app.ai.AiOutcome
import com.knowme.app.data.db.AppDatabase
import com.knowme.app.data.db.DigestEntity
import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.data.db.Priority
import com.knowme.app.data.db.TodoEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 早报生成的结果状态。 */
sealed interface DigestResult {
    data class Ok(val handled: Int, val noiseFolded: Int, val todos: Int) : DigestResult
    data class Error(val message: String) : DigestResult
}

/**
 * 每日早报：把当天通知交给用户的 AI，消化成
 * ① 一段叙事 ② 每条通知的优先级/摘要 ③ 抽取的待办。
 */
class DigestGenerator(
    private val db: AppDatabase,
    private val chat: suspend (system: String, user: String) -> AiOutcome,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generateForToday(): DigestResult {
        val (start, end) = dayRange()
        val dateKey = dateKey()
        val notifications = db.notificationDao().between(start, end)
        if (notifications.isEmpty()) return DigestResult.Error("今天还没有通知可消化。")

        val outcome = chat(SYSTEM_PROMPT, buildUserPrompt(notifications))
        val raw = when (outcome) {
            is AiOutcome.Ok -> outcome.text
            is AiOutcome.Error -> return DigestResult.Error(outcome.message)
        }

        val parsed = runCatching { json.decodeFromString<DigestPayload>(extractJson(raw)) }
            .getOrElse { return DigestResult.Error("AI 返回格式无法解析。原文：${raw.take(200)}") }

        // 回填每条通知的优先级与摘要
        val validIds = notifications.map { it.id }.toSet()
        parsed.items.forEach { item ->
            if (item.id in validIds) {
                db.notificationDao().setAnalysis(
                    id = item.id,
                    priority = item.priority.toPriority(),
                    summary = item.summary?.takeIf { it.isNotBlank() },
                )
            }
        }

        // 重写当天自动待办，去重后写入
        db.todoDao().deleteAutoUndoneSince(start)
        var todoCount = 0
        parsed.todos.forEach { t ->
            if (t.content.isNotBlank() && db.todoDao().countOpenWithContent(t.content) == 0) {
                db.todoDao().insert(
                    TodoEntity(
                        content = t.content.trim(),
                        sourceNotificationId = t.sourceId,
                        sourceLabel = t.sourceLabel,
                        createdAt = System.currentTimeMillis(),
                    )
                )
                todoCount++
            }
        }

        val handled = parsed.items.count { it.priority.equals("HIGH", true) }
        val noise = parsed.items.count { it.priority.equals("LOW", true) }
        db.digestDao().upsert(
            DigestEntity(
                dateKey = dateKey,
                narrative = parsed.narrative.trim(),
                notificationCount = notifications.size,
                noiseFolded = noise,
                generatedAt = System.currentTimeMillis(),
            )
        )
        return DigestResult.Ok(handled = handled, noiseFolded = noise, todos = todoCount)
    }

    private fun buildUserPrompt(notifications: List<NotificationEntity>): String {
        val lines = notifications.joinToString("\n") { n ->
            "id=${n.id} | ${n.appName} | ${n.title} | ${n.text.take(140)}"
        }
        return """
            下面是我今天的手机通知（id | 来源App | 标题 | 正文）：
            $lines

            请输出 JSON，结构严格如下，不要任何额外文字：
            {
              "narrative": "一段50字内的中文叙事，概括今天发生了什么",
              "items": [{"id": 数字, "priority": "HIGH|MID|LOW", "summary": "一句话摘要"}],
              "todos": [{"content": "需要我做的事", "sourceId": 数字, "sourceLabel": "如 微信·王总 09:12"}]
            }
            分级标准：HIGH=需要我亲自处理/回复/有截止；MID=知道就行（日程/快递/账单）；LOW=噪音（促销/砍价/无关推送）。
            todos 只放真正需要我行动的；纯噪音不要进 items 的 summary 也可简略。
        """.trimIndent()
    }

    /** 容错地从模型输出里抠出 JSON 主体。 */
    private fun extractJson(raw: String): String {
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        return if (s >= 0 && e > s) raw.substring(s, e + 1) else raw
    }

    @Serializable
    private data class DigestPayload(
        val narrative: String = "",
        val items: List<Item> = emptyList(),
        val todos: List<Todo> = emptyList(),
    )

    @Serializable
    private data class Item(val id: Long, val priority: String = "MID", val summary: String? = null) {
        fun toPriority(): Priority = priority.toPriority()
    }

    @Serializable
    private data class Todo(val content: String, val sourceId: Long? = null, val sourceLabel: String? = null)

    companion object {
        private const val SYSTEM_PROMPT =
            "你是用户的私人通知管家。任务是把一堆杂乱通知消化成清晰的每日早报。只输出 JSON，不要解释。"

        private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun dateKey(millis: Long = System.currentTimeMillis()): String = dateFmt.format(millis)

        /** 当天 [00:00, 23:59:59] 的毫秒区间。 */
        fun dayRange(millis: Long = System.currentTimeMillis()): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            return start to (start + 24L * 3600 * 1000 - 1)
        }
    }
}

private fun String.toPriority(): Priority =
    when (uppercase()) {
        "HIGH" -> Priority.HIGH
        "LOW" -> Priority.LOW
        "MID" -> Priority.MID
        else -> Priority.MID
    }
