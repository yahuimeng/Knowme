package com.knowme.app.notification

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.provider.Settings

/**
 * 后台保活引导。国产 ROM 的"自启动/后台运行"白名单无法由 App 代码开启，
 * 只能跳到相应设置页，由用户手动允许。
 */
object BackgroundGuide {

    /** 打开"电池优化"列表，让用户把 Knowme 设为不优化/无限制。 */
    fun openBatterySettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { openAppDetails(context) }
    }

    /** 打开本应用的"应用详情"页——国产 ROM 的自启动开关通常在这里能找到。 */
    fun openAppDetails(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
