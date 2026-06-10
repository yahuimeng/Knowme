package com.knowme.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.knowme.app.notification.NotificationAccess

/** 首次启动引导：本地承诺 + 两步授权。 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(NotificationAccess.isGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = NotificationAccess.isGranted(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text("🦞", style = MaterialTheme.typography.headlineSmall)
        Text("欢迎来到 Knowme", style = MaterialTheme.typography.headlineSmall)
        Text(
            "你的通知，从此有人替你看。\n完全在本机运行 · 没有服务器 · 我们碰不到你的数据。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        StepCard(
            index = "第 1 步",
            title = "开启通知读取",
            desc = "让 Knowme 能看到你的通知。这是它工作的前提。",
            done = granted,
        ) {
            OutlinedButton(onClick = { NotificationAccess.openSettings(context) }) {
                Text(if (granted) "已授权 ✓" else "去授权")
            }
        }

        StepCard(
            index = "第 2 步",
            title = "接入你的 AI",
            desc = "进入后到「我的 → AI 服务」，填你自己的 key。我们不经手、不上传。",
            done = false,
        ) {}

        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text(if (granted) "开始使用" else "先进去看看")
        }
        TextButton(onClick = onDone) { Text("跳过引导") }
    }
}

@Composable
private fun StepCard(
    index: String,
    title: String,
    desc: String,
    done: Boolean,
    action: @Composable () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(index, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodyMedium)
            action()
        }
    }
}
