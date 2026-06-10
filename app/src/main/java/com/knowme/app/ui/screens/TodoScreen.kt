package com.knowme.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.knowme.app.data.db.TodoEntity
import com.knowme.app.ui.MainViewModel

@Composable
fun TodoScreen(vm: MainViewModel) {
    val todos by vm.todos.collectAsState()
    val open = todos.filter { !it.done }
    val done = todos.filter { it.done }
    var showDone by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("待办", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${open.size} 未完成",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (todos.isEmpty()) {
            item {
                Column(Modifier.padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗒️", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无待办", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "生成早报时，真正需要你做的事才会被挑出来放这里——抽错了可以直接删。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(open, key = { it.id }) { todo ->
            TodoCard(todo, onToggle = { vm.toggleTodo(todo) }, onDelete = { vm.deleteTodo(todo) })
        }

        if (done.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { showDone = !showDone },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "已完成 ${done.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(if (showDone) "收起 ▴" else "展开 ▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showDone) {
                items(done, key = { it.id }) { todo ->
                    TodoCard(todo, onToggle = { vm.toggleTodo(todo) }, onDelete = { vm.deleteTodo(todo) })
                }
            }
        }
        item { Spacer(Modifier.height(100.dp)) }  // 给磨砂底栏留出空间
    }
}

@Composable
private fun TodoCard(todo: TodoEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = todo.done, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    todo.content,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (todo.done) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                    color = if (todo.done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                todo.sourceLabel?.let {
                    Text(
                        "来自 $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "删除",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
