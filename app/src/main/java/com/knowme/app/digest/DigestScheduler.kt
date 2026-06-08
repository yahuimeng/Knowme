package com.knowme.app.digest

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** 安排每日早报：每天一次，默认早 8 点附近触发。 */
object DigestScheduler {
    private const val WORK_NAME = "knowme_daily_digest"

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
