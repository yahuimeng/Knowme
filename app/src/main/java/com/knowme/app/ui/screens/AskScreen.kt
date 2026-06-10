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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.knowme.app.data.db.ChatMessageEntity
import com.knowme.app.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskScreen(vm: MainViewModel) {
    val conversations by vm.conversations.collectAsState()
    val current by vm.currentConversation.collectAsState()
    val messages by vm.currentMessages.collectAsState()
    val sending by vm.sending.collectAsState()
    var input by remember { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    var showNewMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // 进入页面若没选中对话，自动选最近的
    LaunchedEffect(conversations) {
        if (vm.currentConvId.value == null && conversations.isNotEmpty()) {
            vm.selectConversation(conversations.first().id)
        }
    }
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // 顶部：标题(点开对话列表) + 新对话
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.weight(1f).clickable { showSheet = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "对话列表")
                Spacer(Modifier.size(8.dp))
                Text(
                    current?.title ?: "问问",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            IconButton(onClick = { showNewMode = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新对话")
            }
        }
        current?.let {
            Text(
                if (it.mode == "NOTIFICATION") "🔔 问通知 · 只依据你的本地通知作答" else "💬 自由聊天",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (current == null) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("💬", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("开始一段对话", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = { showNewMode = true }) { Text("新对话") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages, key = { it.id }) { m -> MessageBubble(m) }
                item { Spacer(Modifier.height(4.dp)) }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 92.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (current?.mode == "NOTIFICATION") "问问你的通知…" else "随便聊点什么…") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                )
                FilledTonalIconButton(
                    onClick = { vm.sendChat(input); input = "" },
                    enabled = input.isNotBlank() && !sending,
                    modifier = Modifier.size(52.dp),
                ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送") }
            }
        }
    }

    // 对话列表
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable { showSheet = false; showNewMode = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("新对话", style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
                conversations.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.selectConversation(c.id); showSheet = false }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (c.mode == "NOTIFICATION") "🔔" else "💬")
                        Spacer(Modifier.size(10.dp))
                        Text(
                            c.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            fontWeight = if (c.id == current?.id) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        IconButton(onClick = { vm.deleteConversation(c.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // 新对话：选模式
    if (showNewMode) {
        AlertDialog(
            onDismissRequest = { showNewMode = false },
            title = { Text("新对话") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = { vm.newConversation("CHAT"); showNewMode = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("💬 自由聊天") }
                    OutlinedButton(
                        onClick = { vm.newConversation("NOTIFICATION"); showNewMode = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("🔔 问通知（只依据通知）") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showNewMode = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun MessageBubble(m: ChatMessageEntity) {
    val isUser = m.role == "user"
    val pending = !isUser && m.content.isBlank() && !m.isError
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (pending) {
            Row(
                Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("  思考中…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val bg = when {
                isUser -> MaterialTheme.colorScheme.primary
                m.isError -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val fg = when {
                isUser -> MaterialTheme.colorScheme.onPrimary
                m.isError -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
            val shape = if (isUser) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
            Text(
                m.content,
                modifier = Modifier.widthIn(max = 300.dp).clip(shape).background(bg).padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal,
                color = fg,
            )
        }
    }
}
