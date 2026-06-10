package com.knowme.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.knowme.app.ai.AiBackend
import com.knowme.app.ai.AiOutcome
import com.knowme.app.ai.AiProfile
import com.knowme.app.data.db.DailyTokens
import com.knowme.app.digest.DigestAutoMode
import com.knowme.app.notification.BackgroundGuide
import com.knowme.app.notification.NotificationAccess
import com.knowme.app.ui.MainViewModel
import com.knowme.app.ui.formatTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val profiles by vm.profiles.collectAsState()
    val activeId by vm.activeId.collectAsState()
    val tokens by vm.tokenTotals.collectAsState()
    val tokensToday by vm.tokenTotalsToday.collectAsState()
    val daily by vm.dailyTokens.collectAsState()
    val apps by vm.apps.collectAsState()
    val blocked by vm.blockedPackages.collectAsState()

    var editing by remember { mutableStateOf<AiProfile?>(null) }
    var retention by remember { mutableStateOf(vm.retentionDays) }
    var digestMode by remember { mutableStateOf(vm.digestMode) }
    var interval by remember { mutableStateOf(vm.digestIntervalMin) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("我的", style = MaterialTheme.typography.headlineSmall)

        // ① AI 服务（永远第一，默认展开）
        val active = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        CollapsibleCard(
            title = "🤖 AI 服务",
            summary = active?.takeIf { it.isConfigured }?.let { it.name.ifBlank { it.backend.label } } ?: "未配置",
            initiallyExpanded = true,
        ) {
            val current = editing
            if (current == null) {
                AiList(
                    profiles = profiles,
                    activeId = activeId,
                    onSelect = { vm.setActive(it) },
                    onEdit = { editing = it },
                    onAdd = { editing = AiProfile(id = UUID.randomUUID().toString(), name = "", baseUrl = "", model = "") },
                )
            } else {
                AiEditor(
                    profile = current,
                    isNew = profiles.none { it.id == current.id },
                    vm = vm,
                    onSaved = { editing = null },
                    onCancel = { editing = null },
                    onDeleted = { vm.deleteProfile(current.id); editing = null },
                )
            }
        }

        // ② Token 用量
        CollapsibleCard(
            title = "📊 Token 用量",
            summary = "今日 ${formatTokens(tokensToday.input + tokensToday.output)}",
        ) {
            Text(
                "今日 ${formatTokens(tokensToday.input + tokensToday.output)} · 总计 ${formatTokens(tokens.input + tokens.output)} · 共 ${tokens.calls} 次调用",
                style = MaterialTheme.typography.bodyLarge,
            )
            TokenBarChart(daily)
            Text(
                "近 7 天每日用量。按 AI 接口上报统计，本地模型可能不上报。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ③ 监听哪些 App
        CollapsibleCard(
            title = "📥 监听哪些 App",
            summary = if (blocked.isEmpty()) "全部接收" else "已屏蔽 ${blocked.size} 个",
        ) {
            if (apps.isEmpty()) {
                Text(
                    "等收到通知后，这里会列出各 App，可单独关掉不想看的。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "默认全部接收，关掉的 App 之后的通知将被忽略。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                apps.forEach { app ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${app.count} 条",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = !blocked.contains(app.packageName),
                            onCheckedChange = { receive -> vm.setAppBlocked(app.packageName, !receive) },
                        )
                    }
                }
            }
        }

        // ④ 自动消化
        val modeLabel = when (digestMode) {
            DigestAutoMode.MANUAL -> "手动"
            DigestAutoMode.ON_OPEN -> "打开App时 · ${interval}分钟"
            DigestAutoMode.PERIODIC -> "定时 · ${interval}分钟"
        }
        CollapsibleCard(title = "🔄 自动消化", summary = modeLabel) {
            Text(
                "早报什么时候自动生成。自动模式都按下方间隔节流，且无新通知不消化，避免烧 token。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    DigestAutoMode.MANUAL to "手动",
                    DigestAutoMode.ON_OPEN to "打开App时",
                    DigestAutoMode.PERIODIC to "定时",
                ).forEach { (m, label) ->
                    FilterChip(
                        selected = digestMode == m,
                        onClick = { digestMode = m; vm.setDigestMode(m, interval) },
                        label = { Text(label) },
                    )
                }
            }
            if (digestMode != DigestAutoMode.MANUAL) {
                Text("间隔", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { min ->
                        FilterChip(
                            selected = interval == min,
                            onClick = { interval = min; vm.setDigestMode(digestMode, min) },
                            label = { Text("${min}分钟") },
                        )
                    }
                }
            }
            Text(
                when (digestMode) {
                    DigestAutoMode.MANUAL -> "仅在「今日」页点按钮时生成。"
                    DigestAutoMode.ON_OPEN -> "每次打开「今日」页，若距上次≥间隔且有新通知才自动生成（推荐）。"
                    DigestAutoMode.PERIODIC -> "后台每隔间隔生成一次（系统限制最短 15 分钟，较费电）。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ⑤ 权限与后台（一次性，靠下）
        val granted = NotificationAccess.isGranted(context)
        CollapsibleCard(title = "🔐 权限与后台", summary = if (granted) "已授权" else "未授权") {
            Text(
                if (granted)
                    "通知读取已授权。国产手机还需手动开「自启动」+电池「无限制」，否则息屏后可能漏收。"
                else
                    "先授予「通知读取」才能开始工作；国产手机还要开自启动 + 电池无限制。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { NotificationAccess.openSettings(context) },
                    modifier = Modifier.weight(1f),
                ) { Text("通知") }
                OutlinedButton(
                    onClick = { BackgroundGuide.openBatterySettings(context) },
                    modifier = Modifier.weight(1f),
                ) { Text("电池") }
                OutlinedButton(
                    onClick = { BackgroundGuide.openAppDetails(context) },
                    modifier = Modifier.weight(1f),
                ) { Text("自启动") }
            }
        }

        // ⑥ 隐私与数据（不频繁，最底）
        CollapsibleCard(
            title = "🔒 隐私与数据",
            summary = if (retention == 0) "永久保留" else "保留 ${retention} 天",
        ) {
            Text("· 全部数据仅存在本机，没有服务器。", style = MaterialTheme.typography.bodyMedium)
            Text("· AI 调用由你的手机直接发往你选的服务，开发者不在链路里。", style = MaterialTheme.typography.bodyMedium)
            Text("通知原文保留期", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3 to "3天", 7 to "7天", 30 to "30天", 0 to "永久").forEach { (days, label) ->
                    FilterChip(
                        selected = retention == days,
                        onClick = { retention = days; vm.setRetentionDays(days) },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                "到期只删「通知原文」（含验证码/私聊等隐私）；待办、早报、问答历史、用量都会保留。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { showClearDialog = true }) { Text("清空全部数据") }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("清空全部数据？") },
                text = { Text("将删除本机所有通知、待办、早报与问答历史，不可恢复。AI 配置会保留。") },
                confirmButton = {
                    TextButton(onClick = { vm.clearAllData(); showClearDialog = false }) { Text("清空") }
                },
                dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } },
            )
        }

        Text(
            "Knowme · 替你看通知，慢慢懂你",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(100.dp))  // 给磨砂底栏留出空间
    }
}

/**
 * 可折叠卡片：点标题展开/收起。收起时只显示标题 + 一句摘要，缩短页面长度。
 * 展开状态用 rememberSaveable 记住。
 */
@Composable
private fun CollapsibleCard(
    title: String,
    summary: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!expanded && summary != null) {
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        if (expanded) "  ▴" else "  ▾",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (expanded) content()
        }
    }
}

/** 近 7 天 token 用量柱状图（Compose 原生，无第三方库）。 */
@Composable
private fun TokenBarChart(daily: List<DailyTokens>) {
    val keyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val labelFmt = remember { SimpleDateFormat("M/d", Locale.getDefault()) }
    val bars = remember(daily) {
        (6 downTo 0).map { offset ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val total = daily.firstOrNull { it.day == keyFmt.format(c.time) }?.total ?: 0L
            labelFmt.format(c.time) to total
        }
    }
    val max = (bars.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val barColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { (_, total) ->
                val frac = (total.toFloat() / max).coerceIn(0.04f, 1f)
                Box(
                    Modifier.weight(1f)
                        .fillMaxHeight(frac)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (total > 0) barColor else emptyColor),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            bars.forEach { (label, _) ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 列表视图：使用中状态 + 已保存档案 + 添加。 */
@Composable
private fun AiList(
    profiles: List<AiProfile>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onEdit: (AiProfile) -> Unit,
    onAdd: () -> Unit,
) {
    val active = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
    if (active != null && active.isConfigured) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                "已连接 · ${active.backend.label} · ${active.model}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    } else {
        Text(
            "还没有可用的 AI 服务，添加一个就能开始消化通知。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    profiles.forEach { p ->
        Row(
            Modifier.fillMaxWidth().clickable { onSelect(p.id) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = p.id == (activeId ?: profiles.firstOrNull()?.id), onClick = { onSelect(p.id) })
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(p.model.ifBlank { p.backend.label }, style = MaterialTheme.typography.bodyLarge)
                Text(
                    p.backend.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onEdit(p) }) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑")
            }
        }
    }

    OutlinedButton(onClick = onAdd) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  添加新服务")
    }
}

/** 编辑/新增一份档案。 */
@Composable
private fun AiEditor(
    profile: AiProfile,
    isNew: Boolean,
    vm: MainViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    onDeleted: () -> Unit,
) {
    var backend by remember(profile) { mutableStateOf(profile.backend) }
    var baseUrl by remember(profile) { mutableStateOf(profile.baseUrl) }
    var apiKey by remember(profile) { mutableStateOf(profile.apiKey) }
    var model by remember(profile) { mutableStateOf(profile.model) }
    var keyVisible by remember { mutableStateOf(false) }
    var testResult by remember(profile) { mutableStateOf<String?>(null) }

    // 名称自动用模型名；地址/模型留空时回退到该后端的默认值
    fun build() = profile.copy(
        name = model.trim().ifBlank { backend.label },
        backend = backend,
        baseUrl = baseUrl.trim().ifBlank { backend.defaultBaseUrl },
        apiKey = apiKey.trim(),
        model = model.trim().ifBlank { backend.defaultModel },
    )

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(if (isNew) "新增服务" else "编辑服务", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        if (!isNew) {
            IconButton(onClick = onDeleted) {
                Icon(Icons.Filled.Delete, contentDescription = "删除此服务", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiBackend.entries.forEach { b ->
            FilterChip(
                selected = backend == b,
                // 切后端时清空输入，让默认值以"提示"形式展示
                onClick = { backend = b; baseUrl = ""; model = "" },
                label = { Text(b.label) },
            )
        }
    }
    if (backend == AiBackend.LOCAL) {
        LocalModelConfig(vm = vm, selected = model, onSelect = { model = it })
    } else {
        val fieldShape = RoundedCornerShape(12.dp)
        OutlinedTextField(
            value = baseUrl, onValueChange = { baseUrl = it },
            label = { Text("接口地址") },
            placeholder = { Text(backend.defaultBaseUrl, maxLines = 1, style = MaterialTheme.typography.bodyMedium) },
            shape = fieldShape, modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        OutlinedTextField(
            value = apiKey, onValueChange = { apiKey = it },
            label = { Text("API Key") },
            placeholder = { Text("粘贴你自己的 key", style = MaterialTheme.typography.bodyMedium) },
            shape = fieldShape, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { keyVisible = !keyVisible }) {
                    Icon(
                        if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                    )
                }
            },
        )
        OutlinedTextField(
            value = model, onValueChange = { model = it },
            label = { Text("模型") },
            placeholder = { Text(backend.defaultModel, style = MaterialTheme.typography.bodyMedium) },
            shape = fieldShape, modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = { vm.saveProfile(build()); onSaved() }) { Text("保存") }
        OutlinedButton(onClick = {
            testResult = "测试中…"
            vm.testConnection(build().toConfig()) { outcome ->
                testResult = when (outcome) {
                    is AiOutcome.Ok -> "✅ 连接成功"
                    is AiOutcome.Error -> "❌ ${outcome.message}"
                }
            }
        }) { Text("测试连接") }
        TextButton(onClick = onCancel) { Text("取消") }
    }
    testResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

    Text(
        if (backend == AiBackend.LOCAL) "📴 本地模型完全离线运行，不联网、不花 token。"
        else "🔑 key 用 Android Keystore 加密存在本机，绝不上传。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 本地模型配置：选择模型 / 导入 .gguf / 运行控制 / 获取指引。 */
@Composable
private fun LocalModelConfig(vm: MainViewModel, selected: String, onSelect: (String) -> Unit) {
    val models by vm.localModels.collectAsState()
    val progress by vm.importProgress.collectAsState()
    val loaded by vm.localModelLoaded.collectAsState()
    var msg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            msg = null
            vm.importModel(uri) { r ->
                msg = r.fold({ "✅ 已导入并选用 $it" }, { "❌ ${it.message}" })
                r.getOrNull()?.let { onSelect(it) }
            }
        }
    }
    val importing = progress != null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ① 选择模型
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("选择模型", style = MaterialTheme.typography.titleSmall)
            if (models.isEmpty()) {
                Text(
                    "还没有模型，点下面「导入 .gguf 文件」加一个。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                models.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(m) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == m, onClick = { onSelect(m) })
                        Text(m, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { vm.deleteModel(m) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // ② 导入
        if (importing) {
            val p = progress ?: 0f
            if (p >= 0f) {
                LinearProgressIndicator(progress = { p.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("导入中 ${(p * 100).toInt()}%（大文件较慢，请稍候）", style = MaterialTheme.typography.bodyMedium)
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("导入中…", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            FilledTonalButton(
                onClick = { msg = null; picker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  导入 .gguf 文件")
            }
        }
        msg?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        HorizontalDivider()

        // ③ 运行控制：播放/停止（像音乐播放器）
        if (models.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (loaded) "运行中 · 已常驻内存" else "未启动",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (loaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalIconButton(
                    onClick = {
                        if (loaded) vm.stopLocalModel()
                        else if (selected.isNotBlank()) vm.startLocalModel(selected)
                    },
                    enabled = loaded || selected.isNotBlank(),
                ) {
                    Icon(
                        if (loaded) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (loaded) "停止" else "启动",
                    )
                }
            }
        }
        Text(
            "获取模型：用手机浏览器到 HuggingFace 或镜像站(hf-mirror.com)下载 .gguf 模型，按手机内存选大小" +
                "（越大越准、越慢），下完点上面「导入」选它。实际效果以所选模型的能力为准。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
