package com.knowme.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.data.db.Priority
import com.knowme.app.digest.DigestResult
import com.knowme.app.notification.NotificationAccess
import com.knowme.app.ui.MainViewModel
import com.knowme.app.ui.formatClock
import com.knowme.app.ui.priorityColor

@Composable
fun TodayScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val todays by vm.todayNotifications.collectAsState()
    val digest by vm.todayDigest.collectAsState()
    val running by vm.digestRunning.collectAsState()
    val configured by vm.aiConfig.collectAsState()
    var toast by remember { mutableStateOf<String?>(null) }

    var granted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = NotificationAccess.isGranted(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val high = todays.filter { it.priority == Priority.HIGH }
    val mid = todays.filter { it.priority == Priority.MID }
    val low = todays.filter { it.priority == Priority.LOW }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("今日", style = MaterialTheme.typography.headlineSmall)
        }

        if (!granted) {
            item { PermissionCard { NotificationAccess.openSettings(context) } }
        }

        // 早报叙事卡
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📖 今天到现在", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        digest?.narrative
                            ?: if (todays.isEmpty()) "还没有收到通知。授权后，Knowme 会开始替你看。"
                            else "已收到 ${todays.size} 条通知，还没消化。点下面按钮生成早报，我把它们分成三档。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    digest?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "共 ${it.notificationCount} 条 · 折叠噪音 ${it.noiseFolded} 条",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (running) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.height(0.dp))
                            Text("  正在消化…", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Button(
                            enabled = todays.isNotEmpty(),
                            onClick = {
                                if (!configured.isConfigured) {
                                    toast = "请先到「我的 → AI 服务」填入你的 key。"
                                } else {
                                    vm.generateDigest { r ->
                                        toast = when (r) {
                                            is DigestResult.Ok -> "已生成：要处理 ${r.handled} · 待办 ${r.todos}"
                                            is DigestResult.Error -> r.message
                                        }
                                    }
                                }
                            },
                        ) { Text(if (digest == null) "生成早报" else "重新生成") }
                    }
                    toast?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        bucket("🔴 需要你处理", high, Priority.HIGH)
        bucket("🟡 知道就行", mid, Priority.MID)
        bucket("⚪️ 已折叠噪音", low, Priority.LOW)

        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bucket(
    title: String,
    list: List<NotificationEntity>,
    priority: Priority,
) {
    if (list.isEmpty()) return
    item {
        Text(
            "$title (${list.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
    items(list, key = { it.id }) { n ->
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(priorityColor(priority)),
                )
                Column(Modifier.padding(start = 10.dp).fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(n.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            formatClock(n.postedAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    val main = n.summary ?: listOf(n.title, n.text).filter { it.isNotEmpty() }.joinToString("：")
                    if (main.isNotEmpty()) {
                        Text(main, style = MaterialTheme.typography.bodyLarge, maxLines = 3)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("还差一步：开启通知读取", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Knowme 完全在本机运行、不上传你的数据。需要你在系统设置里授予「通知使用权」，它才能替你看通知。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGrant) { Text("去授权") }
        }
    }
}
