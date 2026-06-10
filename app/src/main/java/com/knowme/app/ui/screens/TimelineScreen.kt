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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.knowme.app.data.db.NotificationEntity
import com.knowme.app.ui.MainViewModel
import com.knowme.app.ui.dayLabel
import com.knowme.app.ui.formatClock

@Composable
fun TimelineScreen(vm: MainViewModel) {
    val notifications by vm.notifications.collectAsState()
    val byDay = notifications.groupBy { dayLabel(it.postedAt) }
    // 每天可折叠：默认只展开"今天"，其余收起（记住用户手动开合）
    val expandedDays = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("时间线", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
        }
        if (notifications.isEmpty()) {
            item {
                Text(
                    "还没有记录。授权通知读取后，每条通知都会在这里留痕——同一个 App 的多条会折叠成一行。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        byDay.forEach { (day, dayItems) ->
            val dayExpanded = expandedDays[day] ?: (day == "今天")
            item(key = "day-$day") {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { expandedDays[day] = !dayExpanded }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        day,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${dayItems.size} 条 ${if (dayExpanded) "▴" else "▾"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (dayExpanded) {
                // 天内按 App 聚合，App 顺序 = 最新一条的先后
                val byApp = dayItems.groupBy { it.packageName }.values.toList()
                items(byApp, key = { it.first().id }) { group -> AppGroupItem(group) }
            }
        }
        item { Spacer(Modifier.height(100.dp)) }  // 给磨砂底栏留出空间
    }
}

/** 同一 App 当天的通知聚合成一组，多条可折叠。 */
@Composable
private fun AppGroupItem(group: List<NotificationEntity>) {
    val latest = group.first()
    val multi = group.size > 1
    var expanded by remember(latest.id) { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = multi) { expanded = !expanded }
            .padding(vertical = 6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${formatClock(latest.postedAt)}  ${latest.appName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (multi) {
                Text(
                    "${group.size}条 ${if (expanded) "▴" else "▾"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!expanded) {
            val line = snippet(latest)
            if (line.isNotEmpty()) {
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (multi) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            group.forEach { n ->
                Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        formatClock(n.postedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val line = snippet(n)
                    if (line.isNotEmpty()) {
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 6.dp))
    }
}

private fun snippet(n: NotificationEntity): String =
    listOf(n.title, n.text).filter { it.isNotEmpty() }.joinToString("：")
