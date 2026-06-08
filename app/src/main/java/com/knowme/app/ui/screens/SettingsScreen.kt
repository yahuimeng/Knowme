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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.knowme.app.ai.AiBackend
import com.knowme.app.ai.AiProfile
import com.knowme.app.ai.AiOutcome
import com.knowme.app.notification.BackgroundGuide
import com.knowme.app.notification.NotificationAccess
import com.knowme.app.ui.MainViewModel
import com.knowme.app.ui.formatTokens
import java.util.UUID

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val profiles by vm.profiles.collectAsState()
    val activeId by vm.activeId.collectAsState()

    // null = 列表视图；非 null = 正在编辑/新增这份档案
    var editing by remember { mutableStateOf<AiProfile?>(null) }
    var retention by remember { mutableStateOf(vm.retentionDays) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("我的", style = MaterialTheme.typography.headlineSmall)

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🤖 AI 服务（你自己的 key）", style = MaterialTheme.typography.titleMedium)
                val current = editing
                if (current == null) {
                    AiList(
                        profiles = profiles,
                        activeId = activeId,
                        onSelect = { vm.setActive(it) },
                        onEdit = { editing = it },
                        onAdd = {
                            editing = AiProfile(
                                id = UUID.randomUUID().toString(),
                                name = "",
                            )
                        },
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
        }

        // ── 通知读取权限 ──
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📱 通知读取", style = MaterialTheme.typography.titleMedium)
                val granted = NotificationAccess.isGranted(context)
                Text(
                    if (granted) "已授权——Knowme 正在替你看通知。" else "未授权——开启后才能开始工作。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { NotificationAccess.openSettings(context) }) {
                    Text(if (granted) "管理权限" else "去授权")
                }
            }
        }

        // ── 后台保活（自启动/电池）──
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔋 后台保活", style = MaterialTheme.typography.titleMedium)
                Text(
                    "授权后系统会自动拉起监听服务。但小米/华为/OPPO/vivo 等会激进清后台，" +
                        "需你手动把 Knowme 加入「自启动」并设为电池「无限制」，否则息屏后可能漏收通知。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { BackgroundGuide.openBatterySettings(context) }) {
                        Text("电池优化")
                    }
                    OutlinedButton(onClick = { BackgroundGuide.openAppDetails(context) }) {
                        Text("应用详情/自启动")
                    }
                }
            }
        }

        // ── token 用量 ──
        val tokens by vm.tokenTotals.collectAsState()
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📊 Token 用量", style = MaterialTheme.typography.titleMedium)
                Text(
                    "累计 ${tokens.calls} 次调用 · 输入 ${formatTokens(tokens.input)} · 输出 ${formatTokens(tokens.output)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "合计约 ${formatTokens(tokens.input + tokens.output)} tokens。按 AI 接口上报统计，本地模型可能不上报。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        // ── 监听哪些 App ──
        val apps by vm.apps.collectAsState()
        val blocked by vm.blockedPackages.collectAsState()
        var showApps by remember { mutableStateOf(false) }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable { showApps = !showApps },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("📥 监听哪些 App", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (blocked.isEmpty()) "全部接收 ▾" else "已屏蔽 ${blocked.size} 个 ▾",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (showApps) {
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
                            color = MaterialTheme.colorScheme.outline,
                        )
                        apps.forEach { app ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(app.appName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${app.count} 条",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                                Switch(
                                    checked = !blocked.contains(app.packageName),
                                    onCheckedChange = { receive ->
                                        vm.setAppBlocked(app.packageName, !receive)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 隐私与数据 ──
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔒 隐私与数据", style = MaterialTheme.typography.titleMedium)
                Text("· 全部数据仅存在本机，没有服务器。", style = MaterialTheme.typography.bodyMedium)
                Text("· AI 调用由你的手机直接发往你选的服务，开发者不在链路里。", style = MaterialTheme.typography.bodyMedium)

                Text("原文保留期", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3 to "3天", 7 to "7天", 30 to "30天", 0 to "永久").forEach { (days, label) ->
                        FilterChip(
                            selected = retention == days,
                            onClick = { retention = days; vm.setRetentionDays(days) },
                            label = { Text(label) },
                        )
                    }
                }
                OutlinedButton(onClick = { showClearDialog = true }) { Text("清空全部数据") }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("清空全部数据？") },
                text = { Text("将删除本机所有通知、待办与早报，不可恢复。AI 配置会保留。") },
                confirmButton = {
                    TextButton(onClick = {
                        vm.clearAllData()
                        showClearDialog = false
                    }) { Text("清空") }
                },
                dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } },
            )
        }

        Text(
            "Knowme · 替你看通知，慢慢懂你",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
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
    // 使用中状态横幅
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
                "已连接 · ${active.name.ifBlank { active.backend.label }} · ${active.model}",
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
                Text(p.name.ifBlank { "未命名" }, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${p.backend.label} · ${p.model}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
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
    var name by remember(profile) { mutableStateOf(profile.name) }
    var backend by remember(profile) { mutableStateOf(profile.backend) }
    var baseUrl by remember(profile) { mutableStateOf(profile.baseUrl) }
    var apiKey by remember(profile) { mutableStateOf(profile.apiKey) }
    var model by remember(profile) { mutableStateOf(profile.model) }
    var keyVisible by remember { mutableStateOf(false) }
    var testResult by remember(profile) { mutableStateOf<String?>(null) }

    fun build() = profile.copy(
        name = name.trim().ifBlank { backend.label },
        backend = backend,
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        model = model.trim(),
    )

    Text(if (isNew) "新增服务" else "编辑服务", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)

    OutlinedTextField(
        value = name, onValueChange = { name = it },
        label = { Text("名称（如：我的Claude / 公司豆包）") },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiBackend.entries.forEach { b ->
            FilterChip(
                selected = backend == b,
                onClick = { backend = b; baseUrl = b.defaultBaseUrl; model = b.defaultModel },
                label = { Text(b.label) },
            )
        }
    }
    OutlinedTextField(
        value = baseUrl, onValueChange = { baseUrl = it },
        label = { Text("接口地址") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
    )
    OutlinedTextField(
        value = apiKey, onValueChange = { apiKey = it },
        label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
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
        label = { Text("模型") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            vm.saveProfile(build())
            onSaved()
        }) { Text("保存") }
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

    if (!isNew) {
        TextButton(onClick = onDeleted) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  删除这个服务", color = MaterialTheme.colorScheme.error)
        }
    }

    Text(
        "🔑 key 用 Android Keystore 加密存在本机，绝不上传。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
    )
}
