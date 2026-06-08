package com.knowme.app.digest

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** 早报的生成方式。 */
enum class DigestAutoMode { MANUAL, ON_OPEN, PERIODIC }

/** 安排早报生成：每日基线 + 可选的定时后台。 */
object DigestScheduler {
    private const val WORK_NAME = "knowme_daily_digest"
    private const val PERIODIC_WORK = "knowme_periodic_digest"

    /** 每日一次的基线（默认早 8 点），任何模式下都保留。 */
    fun schedule(context: Context, hourOfDay: Int = 8) {
        val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis(hourOfDay), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** 根据模式安排/取消定时后台生成。WorkManager 最短周期 15 分钟。 */
    fun applyMode(context: Context, mode: DigestAutoMode, intervalMin: Int) {
        val wm = WorkManager.getInstance(context)
        if (mode == DigestAutoMode.PERIODIC) {
            val minutes = intervalMin.coerceAtLeast(15).toLong()
            val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(minutes, TimeUnit.MINUTES)
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            wm.cancelUniqueWork(PERIODIC_WORK)
        }
    }

    /** 距离下一个 hourOfDay 点的毫秒数。 */
    private fun initialDelayMillis(hourOfDay: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }
}
