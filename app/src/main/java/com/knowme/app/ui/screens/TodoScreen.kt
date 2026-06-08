package com.knowme.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.knowme.app.ui.MainViewModel

@Composable
fun TodoScreen(vm: MainViewModel) {
    val todos by vm.todos.collectAsState()
    val open = todos.count { !it.done }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("待办", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$open 未完成",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        if (todos.isEmpty()) {
            item {
                Text(
                    "暂无待办。每日早报消化通知时，会把「需要你做的事」自动抽取到这里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(todos, key = { it.id }) { todo ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = todo.done, onCheckedChange = { vm.toggleTodo(todo) })
                Column(Modifier.padding(start = 4.dp)) {
                    Text(
                        todo.content,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                        color = if (todo.done) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    todo.sourceLabel?.let {
                        Text(
                            "来自 $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
