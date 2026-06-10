package com.knowme.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.knowme.app.AppContainer
import com.knowme.app.ai.AiConfig
import com.knowme.app.ai.AiOutcome
import com.knowme.app.ai.AiProfile
import com.knowme.app.data.db.AppNotifCount
import com.knowme.app.data.db.ChatMessageEntity
import com.knowme.app.data.db.ConversationEntity
import com.knowme.app.data.db.DailyTokens
import com.knowme.app.data.db.DigestEntity
import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.data.db.TodoEntity
import com.knowme.app.data.db.TokenTotals
import com.knowme.app.digest.DigestAutoMode
import com.knowme.app.digest.DigestGenerator
import com.knowme.app.digest.DigestResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val notificationDao = container.db.notificationDao()
    private val todoDao = container.db.todoDao()
    private val digestDao = container.db.digestDao()
    private val conversationDao = container.db.conversationDao()
    private val messageDao = container.db.messageDao()
    private val today = DigestGenerator.dayRange()

    val notifications: StateFlow<List<NotificationEntity>> =
        notificationDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationCount: StateFlow<Int> =
        notificationDao.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 今日通知（用于「今日」页三档分组）。 */
    val todayNotifications: StateFlow<List<NotificationEntity>> =
        notificationDao.observeDay(today.first, today.second)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 今日早报（AI 消化后的叙事）。 */
    val todayDigest: StateFlow<DigestEntity?> =
        digestDao.observe(DigestGenerator.dateKey())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _digestRunning = MutableStateFlow(false)
    val digestRunning: StateFlow<Boolean> = _digestRunning.asStateFlow()

    val todos: StateFlow<List<TodoEntity>> =
        todoDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openTodoCount: StateFlow<Int> =
        todoDao.observeOpenCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── AI 档案管理 ──
    val profiles: StateFlow<List<AiProfile>> = container.profiles
    val activeId: StateFlow<String?> = container.activeId

    /** 当前使用中的配置（无则 null），供其它页判断是否已配好 AI。 */
    val activeConfig: StateFlow<AiConfig?> =
        combine(container.profiles, container.activeId) { list, id ->
            (list.firstOrNull { it.id == id } ?: list.firstOrNull())?.toConfig()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveProfile(profile: AiProfile) = container.saveProfile(profile)
    fun deleteProfile(id: String) = container.deleteProfile(id)
    fun setActive(id: String) = container.setActive(id)

    // ── 本地模型 ──
    val localModels: StateFlow<List<String>> = container.localModels
    val importProgress: StateFlow<Float?> = container.downloadProgress
    fun importModel(uri: android.net.Uri, onDone: (Result<String>) -> Unit) =
        container.importModel(uri, onDone)
    fun deleteModel(name: String) = container.deleteModel(name)
    val localModelLoaded: StateFlow<Boolean> = container.localModelLoaded
    fun startLocalModel(name: String) = container.startLocalModel(name)
    fun stopLocalModel() = container.stopLocalModel()

    fun testConnection(config: AiConfig, onResult: (AiOutcome) -> Unit) {
        viewModelScope.launch { onResult(container.testConnection(config)) }
    }

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoDao.update(
                todo.copy(
                    done = !todo.done,
                    doneAt = if (!todo.done) System.currentTimeMillis() else null,
                )
            )
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch { todoDao.delete(todo) }
    }

    fun generateDigest(onResult: (DigestResult) -> Unit) {
        if (_digestRunning.value) return
        _digestRunning.value = true
        viewModelScope.launch {
            val result = container.manualGenerate()  // 无新通知则不重复生成
            _digestRunning.value = false
            onResult(result)
        }
    }

    // ── token 用量（总 + 今日）──
    val tokenTotals: StateFlow<TokenTotals> =
        container.db.tokenUsageDao().observeTotals()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TokenTotals(0, 0, 0))
    val tokenTotalsToday: StateFlow<TokenTotals> =
        container.db.tokenUsageDao().observeTotalsSince(today.first)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TokenTotals(0, 0, 0))
    val dailyTokens: StateFlow<List<DailyTokens>> =
        container.db.tokenUsageDao().observeDaily(System.currentTimeMillis() - 7L * 24 * 3600 * 1000)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── 自动消化 ──
    val digestMode: DigestAutoMode get() = container.digestMode
    val digestIntervalMin: Int get() = container.digestIntervalMin
    fun setDigestMode(mode: DigestAutoMode, intervalMin: Int) = container.setDigestMode(mode, intervalMin)

    /** 「打开App自动」：进今日页时按节流+有新通知才静默生成。 */
    fun maybeAutoGenerateOnOpen() {
        viewModelScope.launch {
            if (container.shouldAutoGenerateOnOpen()) generateDigest {}
        }
    }

    // ── 通知来源过滤 ──
    val apps: StateFlow<List<AppNotifCount>> =
        notificationDao.observeApps().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val blockedPackages: StateFlow<Set<String>> = container.blockedPackages
    fun setAppBlocked(pkg: String, blocked: Boolean) = container.setBlocked(pkg, blocked)

    // ── 引导 ──
    val onboarded: Boolean get() = container.onboarded
    fun markOnboarded() { container.onboarded = true }

    // ── 隐私 ──
    val retentionDays: Int get() = container.retentionDays
    fun setRetentionDays(days: Int) { container.retentionDays = days }
    fun clearAllData(onDone: () -> Unit = {}) {
        viewModelScope.launch { container.clearAllData(); onDone() }
    }

    // ── 问问：多对话 + 多轮聊天 ──
    val conversations: StateFlow<List<ConversationEntity>> =
        conversationDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConvId = MutableStateFlow<Long?>(null)
    val currentConvId: StateFlow<Long?> = _currentConvId.asStateFlow()

    val currentConversation: StateFlow<ConversationEntity?> =
        combine(conversations, _currentConvId) { list, id -> list.firstOrNull { it.id == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessageEntity>> =
        _currentConvId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else messageDao.observeByConversation(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    fun selectConversation(id: Long) { _currentConvId.value = id }

    /** 新建对话并切换过去。mode: "CHAT" / "NOTIFICATION"。 */
    fun newConversation(mode: String) {
        viewModelScope.launch { _currentConvId.value = container.newConversation(mode) }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            container.deleteConversation(id)
            if (_currentConvId.value == id) {
                _currentConvId.value = conversations.value.firstOrNull { it.id != id }?.id
            }
        }
    }

    fun sendChat(text: String) {
        val id = _currentConvId.value ?: return
        if (_sending.value || text.isBlank()) return
        _sending.value = true
        viewModelScope.launch {
            container.sendChat(id, text)
            _sending.value = false
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(container) as T
    }
}
