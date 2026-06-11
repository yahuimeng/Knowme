package com.knowme.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val activeConfig by vm.activeConfig.collectAsState()
    var toast by remember { mutableStateOf<String?>(null) }

    var granted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = NotificationAccess.isGranted(context)
                vm.refreshToday()             // 刷新"今天"范围，避免读到昨天
                vm.maybeAutoGenerateOnOpen()  // 「打开App自动」模式按节流静默生成
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val high = todays.filter { it.priority == Priority.HIGH }
    val mid = todays.filter { it.priority == Priority.MID }
    val low = todays.filter { it.priority == Priority.LOW }

    // 三档可折叠：要处理默认展开，噪音默认收起
    var highExpanded by rememberSaveable { mutableStateOf(true) }
    var midExpanded by rememberSaveable { mutableStateOf(true) }
    var lowExpanded by rememberSaveable { mutableStateOf(false) }

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

        // 早报叙事卡（白卡浮在灰底上，与参考一致）
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    // 概览：三档大数字 + 占比条
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatCol(high.size, "要紧", priorityColor(Priority.HIGH))
                        StatCol(mid.size, "留意", priorityColor(Priority.MID))
                        StatCol(low.size, "噪音", priorityColor(Priority.LOW))
                    }
                    Spacer(Modifier.height(12.dp))
                    StackedBar(high.size, mid.size, low.size)
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(14.dp))

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
                        FilledTonalButton(
                            enabled = todays.isNotEmpty(),
                            onClick = {
                                if (activeConfig?.isConfigured != true) {
                                    toast = "请先到「我的 → AI 服务」添加并选择一个服务。"
                                } else {
                                    vm.generateDigest { r ->
                                        toast = when (r) {
                                            is DigestResult.Ok -> "已生成：要处理 ${r.handled} · 待办 ${r.todos}"
                                            is DigestResult.Error -> r.message
                                        }
                                    }
                                }
                            },
                        ) { Text(if (digest == null) "生成早报" else "重新消化") }
                    }
                    toast?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        bucket("🔴 要紧", high, Priority.HIGH, highExpanded) { highExpanded = !highExpanded }
        bucket("🟡 留意", mid, Priority.MID, midExpanded) { midExpanded = !midExpanded }
        bucket("⚪️ 噪音", low, Priority.LOW, lowExpanded) { lowExpanded = !lowExpanded }

        item { Spacer(Modifier.height(100.dp)) }  // 给磨砂底栏留出空间
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bucket(
    title: String,
    list: List<NotificationEntity>,
    priority: Priority,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    if (list.isEmpty()) return
    item {
        Row(
            Modifier.fillMaxWidth().clickable { onToggle() },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "$title (${list.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (expanded) "收起 ▴" else "展开 ▾",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (!expanded) return
    items(list, key = { it.id }) { n ->
        // 每条可点开看全文：默认 3 行，溢出时才出现「展开全文」，点击展开/收起
        var expanded by rememberSaveable(n.id) { mutableStateOf(false) }
        var overflow by remember(n.id) { mutableStateOf(false) }
        val canExpand = overflow || expanded
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .clickable(enabled = canExpand) { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val main = n.summary ?: listOf(n.title, n.text).filter { it.isNotEmpty() }.joinToString("：")
                    if (main.isNotEmpty()) {
                        Text(
                            main,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { if (!expanded) overflow = it.hasVisualOverflow },
                        )
                        if (canExpand) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (expanded) "收起 ▴" else "展开全文 ▾",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            FilledTonalButton(onClick = onGrant) { Text("去授权") }
        }
    }
}

/** 概览的一档：大数字（用优先级色）+ 标签。 */
@Composable
private fun StatCol(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 三档占比条：一条圆角横条按比例分成三段颜色（空时为灰轨）。 */
@Composable
private fun StackedBar(high: Int, mid: Int, low: Int) {
    val total = high + mid + low
    Row(
        Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
    ) {
        if (total == 0) {
            Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
        } else {
            if (high > 0) Box(Modifier.weight(high.toFloat()).fillMaxHeight().background(priorityColor(Priority.HIGH)))
            if (mid > 0) Box(Modifier.weight(mid.toFloat()).fillMaxHeight().background(priorityColor(Priority.MID)))
            if (low > 0) Box(Modifier.weight(low.toFloat()).fillMaxHeight().background(priorityColor(Priority.LOW)))
        }
    }
}
