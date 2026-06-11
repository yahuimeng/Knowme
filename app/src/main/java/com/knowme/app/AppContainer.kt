package com.knowme.app

import android.content.Context
import com.knowme.app.ai.AiBackend
import com.knowme.app.ai.AiConfig
import com.knowme.app.ai.AiOutcome
import com.knowme.app.ai.AiProfile
import com.knowme.app.ai.AiProvider
import com.knowme.app.ai.LocalLlmEngine
import com.knowme.app.ai.LocalModelManager
import com.knowme.app.ai.SecureConfigStore
import com.knowme.app.data.db.AppDatabase
import com.knowme.app.data.db.ChatMessageEntity
import com.knowme.app.data.db.ConversationEntity
import com.knowme.app.digest.DigestAutoMode
import com.knowme.app.digest.DigestGenerator
import com.knowme.app.digest.DigestResult
import com.knowme.app.digest.DigestScheduler
import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.data.db.PrefSignalEntity
import com.knowme.app.learn.PreferenceLearner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 极简服务定位器（不引入 DI 框架）。持有数据库、加密配置、AI 调用入口。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db: AppDatabase = AppDatabase.get(context)
    private val secureStore = SecureConfigStore(context)
    private val prefs = context.getSharedPreferences("knowme_prefs", Context.MODE_PRIVATE)

    // 「越用越懂你」被动学习器
    private val learner = PreferenceLearner(db.prefSignalDao())
    val preferences: Flow<List<PrefSignalEntity>> get() = learner.observe()

    // 端侧本地模型
    private val localEngine = LocalLlmEngine(appContext)
    private val modelManager = LocalModelManager(appContext)
    private val _localModels = MutableStateFlow(modelManager.listModels())
    val localModels: StateFlow<List<String>> = _localModels.asStateFlow()
    private val _downloadProgress = MutableStateFlow<Float?>(null)  // null=空闲
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

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
        // 只删一条：按下标移除首个匹配，避免 id 重复时把多个一起删掉
        val list = _profiles.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        list.removeAt(idx)
        _profiles.value = list
        secureStore.saveProfiles(list)
        if (_activeId.value == id) setActive(list.firstOrNull()?.id)
    }

    fun setActive(id: String?) {
        _activeId.value = id
        secureStore.setActiveId(id)
    }

    /** 用当前使用中的档案发起一次单轮对话，并记录 token 用量（本地模型不计）。 */
    suspend fun chat(systemPrompt: String, userPrompt: String, kind: String = "chat"): AiOutcome {
        val config = activeProfile()?.toConfig()
            ?: return AiOutcome.Error("还没配置 AI：请先到「我的 → AI 服务」添加并选择一个服务。")
        if (!config.isConfigured) return AiOutcome.Error("当前 AI 服务信息不完整，请检查配置。")
        val outcome = runComplete(config, systemPrompt, userPrompt)
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

    /** 按后端类型分发：本地走端侧引擎，其余走 HTTP。 */
    private suspend fun runComplete(config: AiConfig, system: String, user: String): AiOutcome =
        if (config.backend == AiBackend.LOCAL) {
            localEngine.complete(modelManager.pathFor(config.model), system, user)
        } else {
            AiProvider.forBackend(config.backend).complete(config, system, user)
        }

    /** 设置页"测试连接"用：对指定配置发一句最小探活。 */
    suspend fun testConnection(config: AiConfig): AiOutcome {
        if (!config.isConfigured) return AiOutcome.Error("请先填完配置。")
        return runComplete(config, "You are a connectivity probe.", "回复 ok 即可。")
    }

    // ── 本地模型管理 ──
    /** 从系统文件选择器选中的 .gguf 导入。 */
    fun importModel(uri: android.net.Uri, onDone: (Result<String>) -> Unit) {
        if (_downloadProgress.value != null) return  // 正在导入
        _downloadProgress.value = 0f
        scope.launch {
            val result = modelManager.importFrom(appContext.contentResolver, uri) { p -> _downloadProgress.value = p }
            _localModels.value = modelManager.listModels()
            _downloadProgress.value = null
            onDone(result)
        }
    }

    fun deleteModel(name: String) {
        modelManager.delete(name)
        _localModels.value = modelManager.listModels()
    }

    /** 本地模型是否已常驻内存（供播放/停止按钮）。 */
    val localModelLoaded: StateFlow<Boolean> = localEngine.loaded

    /** 主动启动（预加载）指定模型。 */
    fun startLocalModel(modelName: String) {
        scope.launch { localEngine.load(modelManager.pathFor(modelName)) }
    }

    /** 手动停止常驻的本地模型，释放内存。 */
    fun stopLocalModel() {
        scope.launch { localEngine.stop() }
    }

    /** 生成今天的早报；成功后记录时间戳（供自动模式节流）。本地模型走减负(lean)模式。 */
    suspend fun generateDigest(): DigestResult {
        val lean = activeProfile()?.backend == AiBackend.LOCAL
        val profile = learner.buildProfile()       // 注入分类 prompt 的偏好块（可能为空）
        val loved = learner.lovedKeys()             // 常关注来源：被判 LOW 时兜底升档
        val result = DigestGenerator(
            db, lean = lean, profile = profile, lovedKeys = loved,
        ) { s, u -> chat(s, u, "digest") }.generateForToday()
        if (result is DigestResult.Ok) lastDigestAt = System.currentTimeMillis()
        return result
    }

    // ── 越用越懂你：信号采集与查看 ──
    /** 通知被点开/划走（来自 Listener）。 */
    suspend fun recordSignal(pkg: String, appName: String, sender: String?, engaged: Int, ignored: Int) {
        learner.record(pkg, appName, sender, engaged = engaged, ignored = ignored)
    }

    /** 在 App 内展开了某条通知 → 在乎信号（来自 UI）。 */
    suspend fun recordEngagement(n: NotificationEntity) {
        learner.record(n.packageName, n.appName, n.sender, engaged = 1)
    }

    suspend fun resetPreferences() = learner.reset()

    /** 手动「重新生成」：无新通知则不重复消化（除非从未生成过）。 */
    suspend fun manualGenerate(): DigestResult {
        if (lastDigestAt > 0 && !hasNewSinceLastDigest()) {
            return DigestResult.Error("没有新通知，无需重新生成。")
        }
        return generateDigest()
    }

    // ── 问问：多对话 + 多轮聊天 ──
    /** 新建对话，返回 id。mode: "CHAT" 自由聊天 / "NOTIFICATION" 问通知。 */
    suspend fun newConversation(mode: String): Long {
        val now = System.currentTimeMillis()
        return db.conversationDao().insert(
            ConversationEntity(title = DEFAULT_CONVO_TITLE, mode = mode, createdAt = now, updatedAt = now)
        )
    }

    suspend fun deleteConversation(id: Long) {
        db.messageDao().deleteByConversation(id)
        db.conversationDao().delete(id)
    }

    /** 发一条消息：立即落库(答案先空)→ 按模式+历史调 AI → 回填答案。 */
    suspend fun sendChat(conversationId: Long, text: String) {
        val q = text.trim()
        if (q.isEmpty()) return
        val convo = db.conversationDao().get(conversationId) ?: return
        val now = System.currentTimeMillis()
        db.messageDao().insert(ChatMessageEntity(conversationId = conversationId, role = "user", content = q, createdAt = now))
        // 首条消息用作标题
        if (convo.title.isBlank() || convo.title == DEFAULT_CONVO_TITLE) {
            db.conversationDao().rename(conversationId, q.take(16), now)
        } else {
            db.conversationDao().touch(conversationId, now)
        }
        // 占位的助手消息（UI 立即显示"思考中"）
        val pendingId = db.messageDao().insert(
            ChatMessageEntity(conversationId = conversationId, role = "assistant", content = "", createdAt = now + 1)
        )
        val history = db.messageDao().listByConversation(conversationId).filter { it.id != pendingId }
        val (system, userPrompt) = buildChatPrompt(convo.mode, history)
        val kind = if (convo.mode == "NOTIFICATION") "ask" else "chat"
        val (answer, isError) = when (val r = chat(system, userPrompt, kind)) {
            is AiOutcome.Ok -> r.text to false
            is AiOutcome.Error -> r.message to true
        }
        db.messageDao().update(
            ChatMessageEntity(id = pendingId, conversationId = conversationId, role = "assistant", content = answer, isError = isError, createdAt = now + 1)
        )
    }

    private suspend fun buildChatPrompt(mode: String, history: List<ChatMessageEntity>): Pair<String, String> {
        val convo = history.joinToString("\n") { (if (it.role == "user") "用户" else "助手") + "：" + it.content }
        return if (mode == "NOTIFICATION") {
            val since = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
            val recent = db.notificationDao().since(since).take(80)
                .joinToString("\n") { "[${it.appName}] ${it.title} ${it.text}" }
            val system = "你是用户的私人通知助理。只依据下面提供的通知记录回答用户的问题，不要编造；通知里没有的就说不知道。回答简洁。"
            val user = "通知记录：\n$recent\n\n以下是对话，请接着回答最后一个用户问题：\n$convo"
            system to user
        } else {
            val system = "你是有帮助的 AI 助手，用中文清晰、简洁地回答。"
            val user = if (history.size <= 1) (history.lastOrNull()?.content ?: "") else "以下是对话，请接着回答最后一个用户问题：\n$convo"
            system to user
        }
    }

    // ── 自动消化（早报生成方式）──
    var digestMode: DigestAutoMode
        get() = runCatching {
            DigestAutoMode.valueOf(prefs.getString(KEY_DIGEST_MODE, DigestAutoMode.MANUAL.name)!!)
        }.getOrDefault(DigestAutoMode.MANUAL)
        private set(value) = prefs.edit().putString(KEY_DIGEST_MODE, value.name).apply()

    var digestIntervalMin: Int
        get() = prefs.getInt(KEY_DIGEST_INTERVAL, 30)
        private set(value) = prefs.edit().putInt(KEY_DIGEST_INTERVAL, value).apply()

    private var lastDigestAt: Long
        get() = prefs.getLong(KEY_LAST_DIGEST_AT, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_DIGEST_AT, value).apply()

    /** 更新自动模式与间隔，并重排后台任务。 */
    fun setDigestMode(mode: DigestAutoMode, intervalMin: Int) {
        digestMode = mode
        digestIntervalMin = intervalMin
        DigestScheduler.applyMode(appContext, mode, intervalMin)
    }

    /** App 启动时按当前模式恢复后台任务。 */
    fun applyDigestSchedule() {
        DigestScheduler.applyMode(appContext, digestMode, digestIntervalMin)
    }

    private suspend fun hasNewSinceLastDigest(): Boolean =
        db.notificationDao().countSince(lastDigestAt) > 0

    /** 「打开App自动」模式下，进今日页时判断是否该静默生成一次。 */
    suspend fun shouldAutoGenerateOnOpen(): Boolean {
        if (digestMode != DigestAutoMode.ON_OPEN) return false
        if (activeProfile()?.isConfigured != true) return false
        if (System.currentTimeMillis() - lastDigestAt < digestIntervalMin * 60_000L) return false
        return hasNewSinceLastDigest()  // 无新通知则不消化
    }

    /** 后台自动消化：未配置或没有新通知则跳过（不烧 token）。 */
    suspend fun autoGenerateIfNew(): DigestResult? {
        if (activeProfile()?.isConfigured != true) return null
        if (!hasNewSinceLastDigest()) return null
        return generateDigest()
    }

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
        db.conversationDao().clear()
        db.messageDao().clear()
        db.prefSignalDao().clear()
    }

    private companion object {
        const val KEY_RETENTION_DAYS = "retention_days"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_BLOCKED = "blocked_packages"
        const val KEY_DIGEST_MODE = "digest_mode"
        const val KEY_DIGEST_INTERVAL = "digest_interval_min"
        const val KEY_LAST_DIGEST_AT = "last_digest_at"
        const val DEFAULT_RETENTION_DAYS = 7
        const val DEFAULT_CONVO_TITLE = "新对话"
    }
}
