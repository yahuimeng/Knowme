package com.knowme.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.knowme.app.data.db.AskMessageEntity
import com.knowme.app.ui.MainViewModel

@Composable
fun AskScreen(vm: MainViewModel) {
    var question by remember { mutableStateOf("") }
    val history by vm.askHistory.collectAsState()
    val asking by vm.asking.collectAsState()
    val listState = rememberLazyListState()

    // 新消息到达时自动滚到底部
    LaunchedEffect(history.size, asking) {
        val count = history.size + if (asking) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
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
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "清空历史")
                }
            }
        }

        if (history.isEmpty() && !asking) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "用你自己的 AI，追问这些天的通知。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("试试：今天有什么没回的？", style = MaterialTheme.typography.bodyMedium)
                Text("这周谁找我最多？", style = MaterialTheme.typography.bodyMedium)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(history, key = { it.id }) { msg -> QaItem(msg) }
            if (asking) {
                item {
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("  正在思考…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.weight(1f),
                label = { Text("输入问题…") },
                maxLines = 3,
            )
            Button(
                onClick = { vm.ask(question); question = "" },
                enabled = question.isNotBlank() && !asking,
            ) { Text("发送") }
        }
    }
}

@Composable
private fun QaItem(msg: AskMessageEntity) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        // 问题：靠右气泡
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Text(
                    msg.question,
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // 回答：靠左气泡
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (msg.isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            Text(msg.answer, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
