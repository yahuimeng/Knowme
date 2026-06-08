package com.knowme.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.knowme.app.AppContainer
import com.knowme.app.ai.AiConfig
import com.knowme.app.ai.AiOutcome
import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.data.db.TodoEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val notificationDao = container.db.notificationDao()
    private val todoDao = container.db.todoDao()

    val notifications: StateFlow<List<NotificationEntity>> =
        notificationDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationCount: StateFlow<Int> =
        notificationDao.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
