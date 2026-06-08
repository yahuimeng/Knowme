package com.knowme.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.knowme.app.AppContainer
import com.knowme.app.ai.AiConfig
import com.knowme.app.ai.AiOutcome
import com.knowme.app.ai.AiProfile
import com.knowme.app.data.db.AppNotifCount
import com.knowme.app.data.db.AskMessageEntity
import com.knowme.app.data.db.DigestEntity
import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.data.db.TodoEntity
import com.knowme.app.digest.DigestGenerator
import com.knowme.app.digest.DigestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val notificationDao = container.db.notificationDao()
    private val todoDao = container.db.todoDao()
    private val digestDao = container.db.digestDao()
    private val askDao = container.db.askDao()
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

    fun generateDigest(onResult: (DigestResult) -> Unit) {
        if (_digestRunning.value) return
        _digestRunning.value = true
        viewModelScope.launch {
            val result = container.generateDigest()
            _digestRunning.value = false
            onResult(result)
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

    // ── 问问（带历史）──
    val askHistory: StateFlow<List<AskMessageEntity>> =
        askDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _asking = MutableStateFlow(false)
    val asking: StateFlow<Boolean> = _asking.asStateFlow()

    fun ask(question: String) {
        val q = question.trim()
        if (q.isEmpty() || _asking.value) return
        _asking.value = true
        viewModelScope.launch {
            val recent = notificationDao.since(System.currentTimeMillis() - 7L * 24 * 3600 * 1000)
                .take(80)
                .joinToString("\n") { "[${it.appName}] ${it.title} ${it.text}" }
            val system = "你是用户的私人通知助理。只依据下面提供的通知记录回答，不要编造。回答简洁。"
            val user = "通知记录：\n$recent\n\n问题：$q"
            val (answer, isError) = when (val r = container.chat(system, user)) {
                is AiOutcome.Ok -> r.text to false
                is AiOutcome.Error -> r.message to true
            }
            askDao.insert(
                AskMessageEntity(
                    question = q,
                    answer = answer,
                    isError = isError,
                    createdAt = System.currentTimeMillis(),
                )
            )
            _asking.value = false
        }
    }

    fun clearAskHistory() {
        viewModelScope.launch { askDao.clear() }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(container) as T
    }
}
