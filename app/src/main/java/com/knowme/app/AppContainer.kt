package com.knowme.app

import android.content.Context
import com.knowme.app.ai.AiConfig
import com.knowme.app.ai.AiOutcome
import com.knowme.app.ai.AiProvider
import com.knowme.app.ai.SecureConfigStore
import com.knowme.app.data.db.AppDatabase
import com.knowme.app.digest.DigestGenerator
import com.knowme.app.digest.DigestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 极简服务定位器（不引入 DI 框架）。持有数据库、加密配置、AI 调用入口。
 */
class AppContainer(context: Context) {

    val db: AppDatabase = AppDatabase.get(context)
    private val secureStore = SecureConfigStore(context)
    private val prefs = context.getSharedPreferences("knowme_prefs", Context.MODE_PRIVATE)

    private val _aiConfig = MutableStateFlow(secureStore.load())
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()

    fun saveAiConfig(config: AiConfig) {
        secureStore.save(config)
        _aiConfig.value = config
    }

    /** 用当前配置发起一次单轮对话。 */
    suspend fun chat(systemPrompt: String, userPrompt: String): AiOutcome {
        val config = _aiConfig.value
        if (!config.isConfigured) return AiOutcome.Error("还没配置 AI：请先到「我的 → AI 服务」填入你的 key。")
        return AiProvider.forBackend(config.backend).complete(config, systemPrompt, userPrompt)
    }

    /** 设置页"测试连接"用：发一句最小的探活请求。 */
    suspend fun testConnection(config: AiConfig): AiOutcome {
        if (!config.isConfigured) return AiOutcome.Error("请先填完接口地址、key 和模型。")
        return AiProvider.forBackend(config.backend)
            .complete(config, "You are a connectivity probe.", "回复 ok 即可。")
    }

    /** 手动触发：立即生成今天的早报。 */
    suspend fun generateDigest(): DigestResult =
        DigestGenerator(db, ::chat).generateForToday()

    // ── 隐私 / 保留期 ──

    var retentionDays: Int
        get() = prefs.getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)
        set(value) = prefs.edit().putInt(KEY_RETENTION_DAYS, value).apply()

    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** 删除早于保留期的通知原文。retentionDays<=0 表示永久保留。 */
    suspend fun runRetentionCleanup() {
        val days = retentionDays
        if (days <= 0) return
        val before = System.currentTimeMillis() - days * 24L * 3600 * 1000
        db.notificationDao().deleteOlderThan(before)
    }

    /** 一键清空：通知 / 待办 / 早报。 */
    suspend fun clearAllData() {
        db.notificationDao().clear()
        db.todoDao().clear()
        db.digestDao().clear()
    }

    private companion object {
        const val KEY_RETENTION_DAYS = "retention_days"
        const val KEY_ONBOARDED = "onboarded"
        const val DEFAULT_RETENTION_DAYS = 7
    }
}
