package com.knowme.app.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService

/** 通知读取权限的状态检查与跳转设置页。 */
object NotificationAccess {

    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val component = ComponentName(context, KnowmeNotificationListener::class.java)
        return enabled.split(":").any {
            ComponentName.unflattenFromString(it) == component
        }
    }

    /**
     * 主动请求系统重新绑定监听服务。
     *
     * 覆盖安装/更新 App 后，系统不会自动 rebind：`enabled_notification_listeners` 里组件还在
     * （isGranted 仍为 true），但 onNotificationPosted 不再回调，导致新通知静默丢失。
     * 仅在已授权时调用；未授权时是 no-op。冷启动与回前台时各调一次，幂等、开销极小。
     */
    fun requestRebind(context: Context) {
        if (!isGranted(context)) return
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, KnowmeNotificationListener::class.java)
            )
        }
    }

    /** 打开系统"通知使用权"设置页，让用户为 Knowme 授权。 */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
