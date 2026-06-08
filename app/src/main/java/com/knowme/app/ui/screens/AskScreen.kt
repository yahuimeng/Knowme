package com.knowme.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.knowme.app.data.db.AskMessageEntity
import com.knowme.app.ui.MainViewModel

private val suggestions = listOf("今天有什么没回的？", "这周谁找我最多？", "有哪些待办没做？", "最近的快递到了吗？")

@Composable
fun AskScreen(vm: MainViewModel) {
    var question by remember { mutableStateOf("") }
    val history by vm.askHistory.collectAsState()
    val asking by vm.asking.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(history.size, history.lastOrNull()?.answer) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    fun send(text: String) {
        vm.ask(text)
        question = ""
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("问问", style = MaterialTheme.typography.headlineSmall)
            if (history.isNotEmpty()) {
                IconButton(onClick = { vm.clearAskHistory() }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "清空历史", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }

        if (history.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("💬", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("问问你的通知", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "用你自己的 AI，追问这些天发生了什么",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                suggestions.forEach { s ->
                    AssistChip(
                        onClick = { send(s) },
                        label = { Text(s) },
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(history, key = { it.id }) { msg -> QaItem(msg) }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }

        // 输入栏
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("问问这些天的通知…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
            )
            FilledIconButton(
                onClick = { send(question) },
                enabled = question.isNotBlank() && !asking,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
            }
        }
    }
}

@Composable
private fun QaItem(msg: AskMessageEntity) {
    val pending = msg.answer.isBlank() && !msg.isError
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 我的提问（右）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Bubble(
                text = msg.question,
                bg = MaterialTheme.colorScheme.primary,
                fg = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                bold = true,
            )
        }
        // 回答（左）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            if (pending) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("  思考中…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Bubble(
                    text = msg.answer,
                    bg = if (msg.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    fg = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                    bold = false,
                )
            }
        }
    }
}

@Composable
private fun Bubble(text: String, bg: Color, fg: Color, shape: RoundedCornerShape, bold: Boolean) {
    Text(
        text,
        modifier = Modifier
            .widthIn(max = 300.dp)
            .clip(shape)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
        color = fg,
    )
}
