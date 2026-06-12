package com.knowme.app.notification

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.knowme.app.KnowmeApp
import com.knowme.app.data.db.AppDatabase
import com.knowme.app.data.db.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 接管手机全部通知。授权后系统会把每条 posted 通知回调给这里。
 * 我们只抽取标题/正文/来源/时间落地本地库，不做任何上传。
 */
class KnowmeNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 连接断开（系统更新/回收/低内存）时，主动请求重连。
     * 否则 onNotificationPosted 会静默停摆，新通知再也不入库。
     */
    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, KnowmeNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        // 用户在「监听哪些 App」里屏蔽的来源，直接丢弃
        val container = (applicationContext as KnowmeApp).container
        if (container.isBlocked(sbn.packageName)) return
        val entity = sbn.toEntity() ?: return
        scope.launch {
            AppDatabase.get(applicationContext).notificationDao().insert(entity)
        }
    }

    /**
     * 通知被移除：最干净的被动学习信号。
     *  REASON_CLICK  = 用户点开了 → 在乎（engaged）
     *  REASON_CANCEL = 用户划走了 → 多半不在乎（ignored，权重低）
     * 其余原因（系统/App 自行取消）不计入，避免噪音。
     */
    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        sbn ?: return
        val engaged = if (reason == REASON_CLICK) 1 else 0
        val ignored = if (reason == REASON_CANCEL) 1 else 0
        if (engaged == 0 && ignored == 0) return
        val pkg = sbn.packageName ?: return
        val appName = resolveAppName(pkg)
        val sender = sbn.senderOrNull()
        val container = (applicationContext as KnowmeApp).container
        scope.launch { container.recordSignal(pkg, appName, sender, engaged, ignored) }
    }

    private fun StatusBarNotification.toEntity(): NotificationEntity? {
        // 跳过常驻/进行中通知（音乐、下载进度等）与分组汇总，避免噪音
        val flags = notification.flags
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return null
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return null

        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
            ?.toString()?.trim().orEmpty()

        // 标题和正文都空的，多半是无意义系统通知
        if (title.isEmpty() && text.isEmpty()) return null

        return NotificationEntity(
            sbnKey = key ?: "$packageName:$id:$postTime",
            packageName = packageName,
            appName = resolveAppName(packageName),
            title = title,
            text = text,
            postedAt = if (postTime > 0) postTime else System.currentTimeMillis(),
            sender = senderOrNull(),
            category = notification.category,
        )
    }

    /** 抽发信人/来源：优先会话标题，其次标题（IM 场景标题通常是对方名字）。 */
    private fun StatusBarNotification.senderOrNull(): String? {
        val extras = notification.extras ?: return null
        val conv = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
        if (!conv.isNullOrEmpty()) return conv
        return extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun resolveAppName(pkg: String): String = runCatching {
        val pm = applicationContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
