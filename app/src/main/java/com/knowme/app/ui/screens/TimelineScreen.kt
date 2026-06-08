package com.knowme.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.knowme.app.ui.MainViewModel
import com.knowme.app.ui.dayLabel
import com.knowme.app.ui.formatClock

@Composable
fun TimelineScreen(vm: MainViewModel) {
    val notifications by vm.notifications.collectAsState()
    // 按"今天/昨天/日期"分组
    val grouped = notifications.groupBy { dayLabel(it.postedAt) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("时间线", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
        }
        if (notifications.isEmpty()) {
            item {
                Text(
                    "还没有记录。授权通知读取后，你的每条通知都会在这里留痕——这是你这一天的行车记录仪。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        grouped.forEach { (day, items) ->
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    day,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(items, key = { it.id }) { n ->
                // 点击展开/收起：长通知默认折叠 3 行，点一下看全文
                var expanded by remember(n.id) { mutableStateOf(false) }
                var overflowing by remember(n.id) { mutableStateOf(false) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 6.dp),
                ) {
                    Text(
                        "${formatClock(n.postedAt)}  ${n.appName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    val line = listOf(n.title, n.text).filter { it.isNotEmpty() }.joinToString("：")
                    if (line.isNotEmpty()) {
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                if (!expanded) overflowing = result.hasVisualOverflow
                            },
                        )
                        if (overflowing || expanded) {
                            Text(
                                if (expanded) "收起 ▴" else "展开 ▾",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 6.dp))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
