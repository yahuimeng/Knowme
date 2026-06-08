package com.knowme.app.digest

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.knowme.app.KnowmeApp

/** 每天定时把当天通知消化成早报，并顺手做隐私保留期清理。 */
class DailyDigestWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as KnowmeApp).container
        // 先清理过期原文（隐私保留期）
        container.runRetentionCleanup()
        // 未配置 / 无新通知则安静跳过，不烧 token
        return when (container.autoGenerateIfNew()) {
            is DigestResult.Error -> Result.retry()
            else -> Result.success()  // Ok 或 null(跳过) 都算成功
        }
    }
}
