package com.knowme.app.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

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

    /** 打开系统"通知使用权"设置页，让用户为 Knowme 授权。 */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
