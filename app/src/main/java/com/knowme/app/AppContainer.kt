package com.knowme.app

import android.content.Context
import com.knowme.app.ai.AiConfig
import com.knowme.app.ai.AiOutcome
import com.knowme.app.ai.AiProfile
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

    private val _profiles = MutableStateFlow(secureStore.loadProfiles())
    val profiles: StateFlow<List<AiProfile>> = _profiles.asStateFlow()

    private val _activeId = MutableStateFlow(
        secureStore.loadActiveId() ?: secureStore.loadProfiles().firstOrNull()?.id
    )
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    fun activeProfile(): AiProfile? =
        _profiles.value.firstOrNull { it.id == _activeId.value } ?: _profiles.value.firstOrNull()

    /** 新增或更新一份档案。 */
    fun saveProfile(profile: AiProfile) {
        val list = _profiles.value.toMutableList()
        val idx = list.indexOfFirst { it.id == profile.id }
        if (idx >= 0) list[idx] = profile else list.add(profile)
        _profiles.value = list
        secureStore.saveProfiles(list)
        // 第一份自动设为使用中
        if (_activeId.value == null) setActive(profile.id)
    }

    fun deleteProfile(id: String) {
        val list = _profiles.value.filterNot { it.id == id }
        _profiles.value = list
        secureStore.saveProfiles(list)
        if (_activeId.value == id) setActive(list.firstOrNull()?.id)
    }

    fun setActive(id: String?) {
        _activeId.value = id
        secureStore.setActiveId(id)
    }

    /** 用当前使用中的档案发起一次单轮对话，并记录 token 用量。 */
    suspend fun chat(systemPrompt: String, userPrompt: String, kind: String = "chat"): AiOutcome {
        val config = activeProfile()?.toConfig()
            ?: return AiOutcome.Error("还没配置 AI：请先到「我的 → AI 服务」添加并选择一个服务。")
        if (!config.isConfigured) return AiOutcome.Error("当前 AI 服务信息不完整，请检查 key 与模型。")
        val outcome = AiProvider.forBackend(config.backend).complete(config, systemPrompt, userPrompt)
        if (outcome is AiOutcome.Ok && (outcome.inputTokens > 0 || outcome.outputTokens > 0)) {
            db.tokenUsageDao().insert(
                com.knowme.app.data.db.TokenUsageEntity(
                    createdAt = System.currentTimeMillis(),
                    kind = kind,
                    model = config.model,
                    inputTokens = outcome.inputTokens,
                    outputTokens = outcome.outputTokens,
                )
            )
        }
        return outcome
    }

    /** 设置页"测试连接"用：对指定配置发一句最小探活。 */
    suspend fun testConnection(config: AiConfig): AiOutcome {
        if (!config.isConfigured) return AiOutcome.Error("请先填完接口地址、key 和模型。")
        return AiProvider.forBackend(config.backend)
            .complete(config, "You are a connectivity probe.", "回复 ok 即可。")
    }

    /** 手动触发：立即生成今天的早报。 */
    suspend fun generateDigest(): DigestResult =
        DigestGenerator(db) { s, u -> chat(s, u, "digest") }.generateForToday()

    // ── 通知来源过滤（默认全收，名单内的被屏蔽）──
    private val _blockedPackages = MutableStateFlow(
        prefs.getStringSet(KEY_BLOCKED, emptySet())!!.toSet()
    )
    val blockedPackages: StateFlow<Set<String>> = _blockedPackages.asStateFlow()

    /** 监听服务直接调用：该包是否被用户屏蔽。 */
    fun isBlocked(pkg: String): Boolean = _blockedPackages.value.contains(pkg)

    fun setBlocked(pkg: String, blocked: Boolean) {
        val set = _blockedPackages.value.toMutableSet()
        if (blocked) set.add(pkg) else set.remove(pkg)
        _blockedPackages.value = set
        prefs.edit().putStringSet(KEY_BLOCKED, set).apply()
    }

    // ── 引导 ──
    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    // ── 隐私 / 保留期 ──
    var retentionDays: Int
        get() = prefs.getInt(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS)
        set(value) = prefs.edit().putInt(KEY_RETENTION_DAYS, value).apply()

    suspend fun runRetentionCleanup() {
        val days = retentionDays
        if (days <= 0) return
        val before = System.currentTimeMillis() - days * 24L * 3600 * 1000
        db.notificationDao().deleteOlderThan(before)
    }

    suspend fun clearAllData() {
        db.notificationDao().clear()
        db.todoDao().clear()
        db.digestDao().clear()
        db.askDao().clear()
        db.tokenUsageDao().clear()
    }

    private companion object {
        const val KEY_RETENTION_DAYS = "retention_days"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_BLOCKED = "blocked_packages"
        const val DEFAULT_RETENTION_DAYS = 7
    }
}
