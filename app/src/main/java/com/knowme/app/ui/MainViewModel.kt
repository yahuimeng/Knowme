package com.knowme.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.knowme.app.AppContainer
import com.knowme.app.ai.AiConfig
import com.knowme.app.ai.AiOutcome
import com.knowme.app.data.db.DigestEntity
import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.data.db.TodoEntity
import com.knowme.app.digest.DigestGenerator
import com.knowme.app.digest.DigestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val notificationDao = container.db.notificationDao()
    private val todoDao = container.db.todoDao()
    private val digestDao = container.db.digestDao()
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

    val aiConfig: StateFlow<AiConfig> = container.aiConfig

    fun saveAiConfig(config: AiConfig) = container.saveAiConfig(config)

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

    // ── 引导 ──
    val onboarded: Boolean get() = container.onboarded
    fun markOnboarded() { container.onboarded = true }

    // ── 隐私 ──
    val retentionDays: Int get() = container.retentionDays
    fun setRetentionDays(days: Int) { container.retentionDays = days }
    fun clearAllData(onDone: () -> Unit = {}) {
        viewModelScope.launch { container.clearAllData(); onDone() }
    }

    fun ask(question: String, onResult: (AiOutcome) -> Unit) {
        viewModelScope.launch {
            val recent = notificationDao.since(System.currentTimeMillis() - 7L * 24 * 3600 * 1000)
                .take(80)
                .joinToString("\n") { "[${it.appName}] ${it.title} ${it.text}" }
            val system = "你是用户的私人通知助理。只依据下面提供的通知记录回答，不要编造。回答简洁。"
            val user = "通知记录：\n$recent\n\n问题：$question"
            onResult(container.chat(system, user))
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(container) as T
    }
}
