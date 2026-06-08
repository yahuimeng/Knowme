package com.knowme.app.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val entity = sbn.toEntity() ?: return
        scope.launch {
            AppDatabase.get(applicationContext).notificationDao().insert(entity)
        }
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
        )
    }

    private fun resolveAppName(pkg: String): String = runCatching {
        val pm = applicationContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
