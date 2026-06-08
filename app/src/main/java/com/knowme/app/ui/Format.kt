package com.knowme.app.ui

import androidx.compose.ui.graphics.Color
import com.knowme.app.data.db.Priority
import com.knowme.app.ui.theme.PriorityHigh
import com.knowme.app.ui.theme.PriorityLow
import com.knowme.app.ui.theme.PriorityMid
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val hm = SimpleDateFormat("HH:mm", Locale.getDefault())
private val md = SimpleDateFormat("M月d日", Locale.getDefault())

fun formatClock(millis: Long): String = hm.format(Date(millis))

/** 大数字友好展示：1234 → 1.2k，1500000 → 1.5M。 */
fun formatTokens(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fk", n / 1_000.0)
    else -> n.toString()
}

/** 相对今天的友好日期分组标签：今天 / 昨天 / M月d日 */
fun dayLabel(millis: Long): String {
    val today = Calendar.getInstance()
    val that = Calendar.getInstance().apply { timeInMillis = millis }
    val sameYear = today.get(Calendar.YEAR) == that.get(Calendar.YEAR)
    val dDay = today.get(Calendar.DAY_OF_YEAR) - that.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dDay == 0 -> "今天"
        sameYear && dDay == 1 -> "昨天"
        else -> md.format(Date(millis))
    }
}

fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> PriorityHigh
    Priority.MID -> PriorityMid
    else -> PriorityLow
}
