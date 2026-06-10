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
    private val lean: Boolean = false,   // 本地小模型：减负模式（只判优先级，不写摘要/叙事/待办）
    private val chat: suspend (system: String, user: String) -> AiOutcome,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generateForToday(): DigestResult {
        val (start, end) = dayRange()
        val dateKey = dateKey()
        val all = db.notificationDao().between(start, end)
        if (all.isEmpty()) return DigestResult.Error("今天还没有通知可消化。")

        // 1) 增量：只处理尚未分类的通知（重生成时老结果保留，省 token）
        val unclassified = all.filter { it.priority == Priority.UNKNOWN }

        // 2) 本地规则预筛：明显噪音本地判 LOW，不送 AI
        val forAi = ArrayList<NotificationEntity>()
        for (n in unclassified) {
            val pre = NotificationClassifier.preClassify(n)
            if (pre != null) db.notificationDao().setAnalysis(n.id, pre, null)
            else forAi.add(n)
        }

        var todoCount = 0
        var aiNarrative: String? = null

        // 3) 只有存在"拿不准"的新通知时才调用 AI
        if (forAi.isNotEmpty()) {
            if (lean) {
                // 本地小模型：减负——只让它判优先级（行格式，最稳），不写摘要/叙事/待办
                val targets = forAi.takeLast(LEAN_MAX)
                val outcome = chat(LEAN_SYSTEM, buildLeanPrompt(targets))
                val raw = when (outcome) {
                    is AiOutcome.Ok -> outcome.text
                    is AiOutcome.Error -> return DigestResult.Error(outcome.message)
                }
                val validIds = targets.map { it.id }.toSet()
                parseLeanPriorities(raw).forEach { (id, p) ->
                    if (id in validIds) db.notificationDao().setAnalysis(id, p, null)
                }
                // 未在本轮判定的（超出 LEAN_MAX 的），先按 MID 兜底，避免一直 UNKNOWN
                forAi.filter { it.id !in validIds }.forEach { db.notificationDao().setAnalysis(it.id, Priority.MID, null) }
            } else {
                // 云端大模型：完整 JSON（分级+摘要+待办+叙事）
                val outcome = chat(SYSTEM_PROMPT, buildUserPrompt(forAi, all.size))
                val raw = when (outcome) {
                    is AiOutcome.Ok -> outcome.text
                    is AiOutcome.Error -> return DigestResult.Error(outcome.message)
                }
                val parsed = runCatching { json.decodeFromString<DigestPayload>(extractJson(raw)) }
                    .getOrElse { return DigestResult.Error("AI 返回格式无法解析。原文：${raw.take(200)}") }

                val validIds = forAi.map { it.id }.toSet()
                parsed.items.forEach { item ->
                    if (item.id in validIds) {
                        db.notificationDao().setAnalysis(
                            id = item.id,
                            priority = item.toPriority(),
                            summary = item.summary?.takeIf { it.isNotBlank() },
                        )
                    }
                }
                parsed.todos.take(5).forEach { t ->
                    val c = t.content.trim()
                    if (c.isNotEmpty() && db.todoDao().countWithContent(c) == 0) {
                        db.todoDao().insert(
                            TodoEntity(
                                content = c,
                                sourceNotificationId = t.sourceId,
                                sourceLabel = t.sourceLabel,
                                createdAt = System.currentTimeMillis(),
                            )
                        )
                        todoCount++
                    }
                }
                aiNarrative = parsed.narrative.trim().takeIf { it.isNotBlank() }
            }
        }

        // 4) 重新读取最新分类，统计并出叙事
        val fresh = db.notificationDao().between(start, end)
        val high = fresh.count { it.priority == Priority.HIGH }
        val noise = fresh.count { it.priority == Priority.LOW }
        val narrative = if (lean) leanNarrative(fresh) else (aiNarrative ?: localNarrative(fresh))
        db.digestDao().upsert(
            DigestEntity(
                dateKey = dateKey,
                narrative = narrative,
                notificationCount = fresh.size,
                noiseFolded = noise,
                generatedAt = System.currentTimeMillis(),
            )
        )
        return DigestResult.Ok(handled = high, noiseFolded = noise, todos = todoCount)
    }

    /**
     * 本地模型的"聚焦总结"：只把判出来的重要几条喂给小模型，让它用一句话概括，
     * 是真内容、不重复数字。失败/离谱则回退本地模板。
     */
    private suspend fun leanNarrative(all: List<NotificationEntity>): String {
        val important = all.filter { it.priority == Priority.HIGH } +
            all.filter { it.priority == Priority.MID }
        if (important.isEmpty()) return localNarrative(all)
        val lines = important.take(10).joinToString("\n") { n ->
            "${n.appName}: ${cleanText("${n.title} ${n.text}").take(40)}"
        }
        val outcome = chat(
            LEAN_SUMMARY_SYSTEM,
            "下面是我今天比较重要的通知。用一句话（40字以内）概括我今天需要关注/处理什么，" +
                "像同事口头跟我汇报，平实直接；不要客套、不要分点、不要解释：\n$lines",
        )
        val text = ((outcome as? AiOutcome.Ok)?.text ?: "").trim().replace(Regex("\\s+"), " ")
        // 质量兜底：空 / 过长（多半跑飞了）→ 回退本地模板
        return if (text.isBlank() || text.length > 80) localNarrative(all) else text
    }

    /** 清洗通知文本：去 URL/方括号/多余空白，便于小模型聚焦。 */
    private fun cleanText(s: String): String =
        s.replace(Regex("https?://\\S+"), "")
            .replace(Regex("[【】\\[\\]<>]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun buildUserPrompt(notifications: List<NotificationEntity>, total: Int): String {
        val lines = notifications.joinToString("\n") { n ->
            "id=${n.id}|${n.appName}|${cleanText("${n.title} ${n.text}").take(80)}"
        }
        return """
            今天共 $total 条通知，其余已本地折叠为噪音。请只判断下面这 ${notifications.size} 条（id|App|内容）：
            $lines

            只输出 JSON，不要任何额外文字：
            {"narrative":"50字内中文，概括今天(可提到共${total}条)","items":[{"id":数字,"priority":"HIGH|MID|LOW","summary":"仅HIGH/MID写一句话，LOW可省略"}],"todos":[{"content":"动宾短语","sourceId":数字,"sourceLabel":"微信·王总 09:12"}]}

            分级标准：HIGH=需我亲自处理/回复/有截止；MID=知道就行(日程/快递/账单/验证码)；LOW=噪音(促销/砍价/无关推送/群闲聊)。
            示例："微信 王总：项目进度发我"→HIGH；"菜鸟 取件码8123"→MID；"某商城 帮我砍一刀"→LOW。
            todos：只抽真正需我主动做的(回复某人/审批/有明确截止)，最多5条，宁缺毋滥；快递到件/账单/促销/闲聊不算。
        """.trimIndent()
    }

    /** 本地小模型用的极简分类 prompt：只要它输出"id 优先级"。 */
    private fun buildLeanPrompt(notifications: List<NotificationEntity>): String {
        val lines = notifications.joinToString("\n") { n ->
            "${n.id} ${n.appName} ${cleanText("${n.title} ${n.text}").take(50)}"
        }
        return """
            给下面每条通知判优先级。只逐行输出，每行格式：id 空格 HIGH或MID或LOW。不要解释、不要别的字。
            HIGH=需我亲自回复/处理/有截止；MID=知道就行(快递/账单/验证码/日程)；LOW=噪音(促销/砍价/群闲聊/无关推送)。
            示例：
            12 HIGH
            34 LOW
            通知：
            $lines
        """.trimIndent()
    }

    /** 容错解析"id 优先级"行：每行取第一个数字 + HIGH/MID/LOW。 */
    private fun parseLeanPriorities(raw: String): List<Pair<Long, Priority>> {
        val numRe = Regex("\\d+")
        val priRe = Regex("HIGH|MID|LOW", RegexOption.IGNORE_CASE)
        val out = ArrayList<Pair<Long, Priority>>()
        raw.lineSequence().forEach { line ->
            val id = numRe.find(line)?.value?.toLongOrNull() ?: return@forEach
            val pri = priRe.find(line)?.value ?: return@forEach
            out.add(id to pri.uppercase().toPriority())
        }
        return out
    }

    /** 不调用 AI 时，本地拼一段叙事（0 token）。 */
    private fun localNarrative(all: List<NotificationEntity>): String {
        val h = all.count { it.priority == Priority.HIGH }
        val m = all.count { it.priority == Priority.MID }
        val l = all.count { it.priority == Priority.LOW }
        return buildString {
            append("今天共 ${all.size} 条通知")
            if (h > 0) append("，${h} 条需要你处理")
            if (m > 0) append("，${m} 条知道就行")
            if (l > 0) append("，其余 $l 条为噪音已折叠")
            append("。")
        }
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
        private const val LEAN_SYSTEM =
            "你是通知优先级分类器。严格按要求逐行输出，不要任何多余文字。"
        private const val LEAN_SUMMARY_SYSTEM =
            "你是简洁的汇报助手。只输出一句话概括，不要分点、不要解释、不要客套。"
        private const val LEAN_MAX = 25   // 本地小模型一次最多判这么多条，避免撑爆/变慢

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
