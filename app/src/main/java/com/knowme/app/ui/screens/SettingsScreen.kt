package com.knowme.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.knowme.app.ai.AiBackend
import com.knowme.app.ai.AiConfig
import com.knowme.app.ai.AiOutcome
import com.knowme.app.notification.NotificationAccess
import com.knowme.app.ui.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val saved by vm.aiConfig.collectAsState()

    var backend by remember(saved) { mutableStateOf(saved.backend) }
    var baseUrl by remember(saved) { mutableStateOf(saved.baseUrl) }
    var apiKey by remember(saved) { mutableStateOf(saved.apiKey) }
    var model by remember(saved) { mutableStateOf(saved.model) }
    var keyVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var retention by remember { mutableStateOf(vm.retentionDays) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun current() = AiConfig(backend, baseUrl.trim(), apiKey.trim(), model.trim())

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("我的", style = MaterialTheme.typography.headlineSmall)

        // ── AI 服务（BYOK）──
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🤖 AI 服务（你自己的 key）", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiBackend.entries.forEach { b ->
                        FilterChip(
                            selected = backend == b,
                            onClick = {
                                backend = b
                                // 切换后端时，把默认地址/模型带上（用户仍可改）
                                baseUrl = b.defaultBaseUrl
                                model = b.defaultModel
                            },
                            label = { Text(b.label) },
                        )
                    }
                }

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("接口地址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
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
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        vm.saveAiConfig(current())
                        testResult = "已保存"
                    }) { Text("保存") }
                    OutlinedButton(onClick = {
                        testResult = "测试中…"
                        vm.testConnection(current()) { outcome ->
                            testResult = when (outcome) {
                                is AiOutcome.Ok -> "✅ 连接成功"
                                is AiOutcome.Error -> "❌ ${outcome.message}"
                            }
                        }
                    }) { Text("测试连接") }
                }
                testResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

                Text(
                    "🔑 key 用 Android Keystore 加密存在本机，绝不上传、绝不写日志。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
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
                Text(
                    "超过保留期的通知原文会自动删除，只留下消化后的摘要与待办。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )

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
                        vm.clearAllData { testResult = "已清空本机数据" }
                        showClearDialog = false
                    }) { Text("清空") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("取消") }
                },
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
