package com.knowme.app.learn

import com.knowme.app.data.db.PrefSignalDao
import com.knowme.app.data.db.PrefSignalEntity
import kotlinx.coroutines.flow.Flow

/**
 * 「越用越懂你」的被动学习器（L0）。
 *
 * 它不需要用户配置任何东西——只从日常行为里悄悄学：用户点开/在 App 里展开了哪条
 * （engaged），把哪条划走了（ignored）。按"来源"聚合成偏好信号，再以两种方式反哺分类：
 *  1) buildProfile()：压缩成一小段中文，注入消化时的分类 prompt，让 AI 据此判档（主力）。
 *  2) lovedKeys()：常关注的来源集合，用于"被判 LOW 时兜底升一档"，绝不本地把消息藏掉。
 *
 * 全部本地、可在「我的」查看并一键重置。
 */
class PreferenceLearner(private val dao: PrefSignalDao) {

    /** 累计一次互动。sender 为空时只记 App 级；非空时同时记 App 级与来源级。 */
    suspend fun record(
        packageName: String,
        appName: String,
        sender: String?,
        engaged: Int = 0,
        ignored: Int = 0,
        now: Long = System.currentTimeMillis(),
    ) {
        if (packageName.isBlank() || (engaged == 0 && ignored == 0)) return
        val app = appName.ifBlank { packageName }
        bump("app:$packageName", "APP", packageName, app, engaged, ignored, now)
        val src = sender?.trim()?.takeIf { it.isNotEmpty() && it != app } ?: return
        bump("src:$packageName|$src", "SENDER", packageName, "$app·$src", engaged, ignored, now)
    }

    private suspend fun bump(
        key: String, kind: String, pkg: String, label: String,
        de: Int, di: Int, now: Long,
    ) {
        dao.ensure(PrefSignalEntity(key = key, kind = kind, packageName = pkg, label = label, updatedAt = now))
        dao.bump(key, de, di, label, now)
    }

    /** 注入分类 prompt 的偏好块；信号不足时返回空串（行为与未学习时一致）。 */
    suspend fun buildProfile(): String {
        val s = summarize(dao.all())
        if (s.loved.isEmpty() && s.muted.isEmpty()) return ""
        return buildString {
            if (s.loved.isNotEmpty()) {
                append("特别关注（请优先判要紧/留意，别折叠）：")
                append(s.loved.joinToString("、"))
                append("。")
            }
            if (s.muted.isNotEmpty()) {
                if (s.loved.isNotEmpty()) append("\n")
                append("基本无视（多为噪音）：")
                append(s.muted.joinToString("、"))
                append("。")
            }
        }
    }

    /** 常关注来源的 key 集合（"src:pkg|sender"），用于兜底升档。 */
    suspend fun lovedKeys(): Set<String> =
        dao.all().filter { it.kind == "SENDER" && it.engaged >= LOVED_MIN && it.engaged > it.ignored * 2 }
            .map { it.key }.toSet()

    fun observe(): Flow<List<PrefSignalEntity>> = dao.observeAll()

    suspend fun reset() = dao.clear()

    /** 常关注来源的标签 + 常忽略 App 的标签（给面板与 prompt 共用，单一口径）。 */
    data class Summary(val loved: List<String>, val muted: List<String>)

    companion object {
        private const val LOVED_MIN = 3   // 常关注：engaged 达到此值且明显多于 ignored
        private const val MUTED_MIN = 5   // 常忽略：从不点开且划走达到此值
        private const val TOP_N = 5

        fun summarize(rows: List<PrefSignalEntity>): Summary {
            val loved = rows.filter { it.kind == "SENDER" && it.engaged >= LOVED_MIN && it.engaged > it.ignored * 2 }
                .sortedByDescending { it.engaged - it.ignored }
                .take(TOP_N).map { it.label }
            val muted = rows.filter { it.kind == "APP" && it.engaged == 0 && it.ignored >= MUTED_MIN }
                .sortedByDescending { it.ignored }
                .take(TOP_N).map { it.label }
            return Summary(loved, muted)
        }
    }
}
